package dev.ivfrost.hydro_backend.devices.internal;

import com.github.benmanes.caffeine.cache.RemovalCause;
import dev.ivfrost.hydro_backend.common.RestResponsePage;
import dev.ivfrost.hydro_backend.config.DeviceProperties;
import dev.ivfrost.hydro_backend.config.CommandGateway;
import dev.ivfrost.hydro_backend.devices.DeviceCommandAction;
import dev.ivfrost.hydro_backend.devices.DeviceCommandCause;
import dev.ivfrost.hydro_backend.devices.DeviceCommandRequest;
import dev.ivfrost.hydro_backend.devices.DeviceKeyEncryptionUtil;
import dev.ivfrost.hydro_backend.devices.DeviceUnlinkRequest;
import dev.ivfrost.hydro_backend.devices.SecretRotatedEvent;
import dev.ivfrost.hydro_backend.devices.AdminDeviceUpdateRequest;
import dev.ivfrost.hydro_backend.devices.DeviceFetchException;
import dev.ivfrost.hydro_backend.devices.DeviceLinkException;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceMapper;
import dev.ivfrost.hydro_backend.devices.DeviceNotFoundException;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionRequest;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionResponse;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.DeviceUpdateRequest;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.ivfrost.hydro_backend.devices.FirmwareVersionComparator;
import dev.ivfrost.hydro_backend.storage.OTAFileUploadEvent;
import dev.ivfrost.hydro_backend.storage.OtaUpdateRecord;
import dev.ivfrost.hydro_backend.storage.OtaUpdateService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceService {

  private static final String DEVICE_NOT_FOUND_KEY_STR = "Device not found for key: ";

  private final DeviceRepository deviceRepository;
  private final DeviceCacheService deviceCacheService;
  private final DeviceKeyEncryptionUtil encryptionUtil;
  private final CacheManager cacheManager;
  private final DeviceMapper deviceMapper;
  private final ObjectMapper objectMapper;
  private final OtaUpdateService otaUpdateService;

  private final DeviceProperties deviceProperties;
  private final CommandGateway commandGateway;
  private final com.github.benmanes.caffeine.cache.Cache<String, String> pendingSecretChanges = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMinutes(5))
      .removalListener((key, value, cause) -> {
        if (cause == RemovalCause.EXPIRED) {
          log.warn("Secret rotation timed out for device: {}", key);
        }
      })
      .build();
  private final RedisTemplate<String, Object> redisTemplate;
  private final DeviceControlPlaneService deviceControlPlaneService;

  /**
   * Registers a device record in this service and generates an ownership secret for linking.
   * Evicts the global device cache. Admin-only call.
   *
   * <p>Operates on the application's <em>device record</em> (identity + linking secret). It does
   * not touch the AWS Thing: the ESP provisions the AWS Thing itself through X.509 fleet
   * provisioning on first boot, which never reaches this endpoint.
   *
   * @param req the device record registration request DTO
   * @return the device record response DTO (with the raw ownership secret)
   */
  @Transactional
  @Caching(evict = {
      @CacheEvict(value = "allDevicesCache", allEntries = true),
  })
  public DeviceProvisionResponse provisionDevice(DeviceProvisionRequest req) {

    Device device = deviceMapper.deviceProvisionRequestToDevice(req);

    // Generate and encrypt the device secret. The fingerprint is a deterministic
    // HMAC tag used for equality lookup, since the ciphertext is randomized.
    String rawSecret = DeviceKeyEncryptionUtil.generateRandomString(32);
    device.setSecret(encryptionUtil.encrypt(rawSecret));
    device.setSecretFingerprint(encryptionUtil.fingerprint(rawSecret));
    Device saved = deviceRepository.save(device);

    // Create the AWS IoT policy for the device which will be attached/unattached on link/unlink
    // to a Cognito ID. Deferred until the transaction actually commits, so a rollback later in
    // this method never leaves AWS state for a device row that ends up not existing.
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        try {
          deviceControlPlaneService.createDevicePolicy(req.key());
        } catch (Exception e) {
          // Creating policy failed and the device row is already committed and permanent at this
          // point. But createDevicePolicy is idempotent, so retrying later is safe.
          log.error("Failed to create IoT policy for device {} after commit. "
              + "Device row exists but has no policy yet.", req.key(), e);
        }
      }
    });

    // Return device details along with the raw secret
    return deviceMapper.deviceToDeviceProvisionResponse(saved, rawSecret);
  }

  /**
   * Registers (or updates) a device record for an ESP32 that is booting, and issues its
   * ownership secret. Called by the platformio post-build hook with a provisioning token
   * only when provisioning mode is enabled.
   *
   * <p>This mirrors a booted device into the application's own data as a <em>device record</em>.
   * It is independent of the AWS Thing: the ESP provisions the AWS Thing itself via X.509 fleet
   * provisioning on first boot, which happens over AWS IoT Core and does not come through this
   * endpoint.
   *
   * @param req the device record registration request DTO
   * @param authorizationHeader the Authorization header holding the provisioning bearer token
   * @return the device record response DTO (with the raw ownership secret)
   */
  @Transactional
  @Caching(evict = {
      @CacheEvict(value = "allDevicesCache", allEntries = true),
      @CacheEvict(value = "deviceByKeyCache", allEntries = true)
  })
  public DeviceProvisionResponse provisionDevice(DeviceProvisionRequest req, String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new BadCredentialsException("Missing or invalid Authorization header");
    }
    String token = authorizationHeader.replace("Bearer ", "").trim();
    String provisioningSecret = deviceProperties.provisioningSecret();
    if (!MessageDigest.isEqual(provisioningSecret.getBytes(StandardCharsets.UTF_8),
        token.getBytes(StandardCharsets.UTF_8))) {
      throw new BadCredentialsException("Invalid provisioning token");
    }

    Device device = deviceMapper.deviceProvisionRequestToDevice(req);

    // Generate and encrypt the device secret. The fingerprint is a deterministic
    // HMAC tag used for equality lookup, since the ciphertext is randomized.
    String rawSecret = DeviceKeyEncryptionUtil.generateRandomString(32);
    device.setSecret(encryptionUtil.encrypt(rawSecret));
    device.setSecretFingerprint(encryptionUtil.fingerprint(rawSecret));

    // Save device
    deviceRepository.upsert(device);

    // Retrieve the saved device
    Device saved = deviceRepository.findByKey(device.getKey())
        .orElseThrow(() -> new EntityNotFoundException("Device not found after upsert"));

    // Create the AWS IoT policy for the device which will be attached/unattached on link/unlink
    // to a Cognito ID. Deferred until the transaction actually commits, so a rollback later in
    // this method never leaves AWS state for a device row that ends up not existing.
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        try {
          deviceControlPlaneService.createDevicePolicy(req.key());
        } catch (Exception e) {
          // Creating policy failed and the device row is already committed and permanent at this
          // point. But createDevicePolicy is idempotent, so retrying later is safe.
          log.error("Failed to create IoT policy for device {} after commit. "
              + "Device row exists but has no policy yet.", req.key(), e);
        }
      }
    });

    // Return device details along with the raw secret
    return deviceMapper.deviceToDeviceProvisionResponse(saved, rawSecret);
  }

  /**
   * Helper method to evict all cache entries related to a specific user ID.
   *
   * @param userId the user ID whose device cache entries should be evicted
   */
  private void evictUserDeviceCache(UUID userId) {
    String pattern = "deviceByUserIdCache::" + userId + "-*";
    Set<String> keys = redisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
      log.debug("Evicted {} cache entries for user {}", keys.size(), userId);
    }
  }

  /**
   * Links an unlinked device to a user using the device secret as ownership proof, and
   * attaches the device's AWS IoT policy to the caller's Cognito identity so the app can
   * subscribe to the device's topics.
   *
   * @param req        the device link request DTO (contains the device secret)
   * @param userSub    the linking user's Cognito User Pool sub, stored as the owner
   * @param identityId the caller's Cognito Identity Pool identity id, resolved server-side,
   *                   used as the IoT policy attach target
   * @param userId     the linking user's application ID
   * @return the updated device response DTO after linking
   * @throws DeviceLinkException     if the device is already linked
   * @throws DeviceNotFoundException if the device is not found
   */
  @Caching(evict = {
      @CacheEvict(value = "deviceByKeyCache", allEntries = true),
      @CacheEvict(value = "allDevicesCache", allEntries = true)
  })
  @Transactional
  public DeviceResponse linkDevice(DeviceLinkRequest req, String userSub, String identityId, UUID userId) {

    // Fetch unlinked device by the deterministic fingerprint of the provided secret
    String fingerprint = encryptionUtil.fingerprint(req.secret());
    Device device = deviceRepository.findBySecretFingerprint(fingerprint)
        .orElseThrow(() -> new DeviceNotFoundException("Device not found"));

    if (device.getUserId() != null) {
      throw new DeviceLinkException("Device is already linked to a user");
    }

    device.setUserId(userId);
    device.setUserSub(userSub);
    device.setLinkedAt(Instant.now());
    device.setDisplayOrder(calculateDeviceOrder(userId));
    deviceRepository.save(device);
    evictUserDeviceCache(userId);

    // identityId is resolved server-side from the caller's ID token, never taken from the
    // request. Attach is deferred to after commit: a rolled-back link must not leave a grant.
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        try {
          deviceControlPlaneService.ensureAndAttachDevicePolicy(device.getKey(), identityId);
        } catch (Exception e) {
          log.error("Failed to attach IoT policy for device {} to identity {} after commit. "
                  + "Link succeeded but the app cannot yet subscribe to this device's topics.",
              device.getKey(), identityId, e);
        }
      }
    });

    return deviceMapper.deviceToDeviceResponse(device);
  }

  /**
   * Unlinks a device from the given user and clears its ownership, so it can be linked
   * again later. Detaches the device's AWS IoT policy from the caller's Cognito identity
   * first, so the app loses access to the device's topics.
   *
   * @param req        the device unlink request DTO (contains the device key)
   * @param identityId the caller's Cognito Identity Pool identity id, resolved server-side,
   *                   used as the IoT policy detach target
   * @param userId     the unlinking user's application ID
   * @throws DeviceNotFoundException if the device is not found
   * @throws DeviceLinkException     if the device does not belong to the user, or the
   *                                 policy detach failed
   */
  @Caching(evict = {
      @CacheEvict(value = "deviceByKeyCache", allEntries = true),
      @CacheEvict(value = "allDevicesCache", allEntries = true)
  })
  @Transactional
  public void unlinkDevice(DeviceUnlinkRequest req, String identityId, UUID userId) {
    Device device = deviceRepository.findByKey(req.deviceKey())
        .orElseThrow(() -> new DeviceNotFoundException(DEVICE_NOT_FOUND_KEY_STR + req.deviceKey()));

    if (device.getUserId() == null || !Objects.equals(device.getUserId(), userId)) {
      throw new DeviceLinkException("Device is not linked to this user");
    }

    // Revoke AWS access first, and synchronously. If this fails, we must not let the
    // DB unlink succeed: revoked in DB but still granted permission on AWS is a security hole.
    try {
      deviceControlPlaneService.detachDevicePolicy(req.deviceKey(), identityId);
    } catch (Exception e) {
      log.error("Failed to detach IoT policy for device {} from identity {}. "
              + "Unlink aborted, device remains linked to prevent stale AWS access.",
          req.deviceKey(), identityId, e);
      throw new DeviceLinkException(
          "Could not unlink device right now, please try again shortly.");
    }

    device.setUserId(null);
    device.setUserSub(null);
    device.setDisplayOrder(0L);
    device.setLinkedAt(null);
    deviceRepository.save(device);
    evictUserDeviceCache(userId);
  }

  /**
   * Verify device ownership
   *
   * @param userId   the user to verify ownership against
   * @param deviceKey the device key to verify ownership of
   * @throws DeviceNotFoundException  if the device is not found
   * @throws IllegalArgumentException if the device does not belong to the specified user
   */
  public void verifyDeviceOwnership(UUID userId, String deviceKey) {
    Device device = requireDeviceByKey(deviceKey);
    if (!Objects.equals(device.getUserId(), userId)) {
      throw new IllegalArgumentException("Device does not belong to the specified user");
    }
  }

  /**
   * Retrieves devices owned by a specific user, by user ID (Admin only, paginated and cached).
   *
   * @param userId the ID of the user whose devices are to be retrieved
   * @return a list of device response DTOs
   * @throws DeviceFetchException if no devices are found for the user
   */
  public Page<DeviceResponse> getDevicesByUserId(UUID userId, Pageable pageable) {
    RestResponsePage<DeviceResponse> devices = deviceCacheService.getDevicesByUserId(userId, pageable);
    log.debug("Fetched {} devices for user ID {}", devices.getContent().size(), userId);
    return devices.map(this::signImageUrl);
  }

  /**
   * Retrieves all device records in the application (Admin only, paginated and cached). These are
   * the app-side rows, distinct from the AWS Things that the devices register via fleet
   * provisioning.
   *
   * @return a list of all device response DTOs
   * @throws DeviceFetchException if no devices are found
   */
  public Page<DeviceResponse> getAllDevices(Pageable pageable) {
    return deviceCacheService.getAllDevices(pageable).map(this::signImageUrl);
  }

  /**
   * Updates fields of a device by its key.
   * Evicts the device's key based cache, the user ID based cache for both the original and
   * new user IDs (if changed) and the global device cache.
   *
   * @param deviceKey the key of the device to update
   * @param req the device update request DTO
   * @param requestingUserId the ID of the currently authenticated user making the request
   * @param newUserId the new user ID to assign to the device (admin only)
   * @param isAdmin whether the request is made by an admin user
   * @return the updated device response DTO
   * @throws DeviceNotFoundException if the device is not found
   * @throws AccessDeniedException   if the device does not belong to the requesting user,
   *                                 or if a non-admin attempts to update restricted fields
   */
  private DeviceResponse doUpdateDeviceDetails(String deviceKey, DeviceUpdateRequest req,
      UUID requestingUserId, UUID newUserId, boolean isAdmin)
      throws AccessDeniedException {
    Device device = requireDeviceByKey(deviceKey);

    String technicalName = req.technicalName();
    String firmware = req.firmware();
    UUID originalUserId = device.getUserId();

    // Verify ownership and guard against non-admin users trying to update restricted fields
    if (!isAdmin) {
      verifyDeviceOwnership(requestingUserId, deviceKey);
      if (technicalName != null || firmware != null || newUserId != null) {
        throw new AccessDeniedException("Non-admin users cannot update technicalName, firmware, or userId");
      }
    }

    // Restricted fields: technicalName, firmware, userId
    if (technicalName != null && !technicalName.isEmpty()) {
      device.setTechnicalName(technicalName);
    }
    if (firmware != null && !firmware.isEmpty()) {
      device.setFirmware(firmware);
    }
    if (newUserId != null) {
      device.setUserId(newUserId);
    }

    // Common fields: friendlyName, location, description, imageUrl, displayOrder.
    // They can be empty; the app falls back to the device key when friendlyName is missing.
    deviceMapper.updateDeviceFromRequest(req, device);

    // The user-device list is cached under keys shaped "<userId>-<pageable>"
    // (see getDevicesByUserId's @Cacheable). The @CacheEvict(key = userId) on the
    // public methods therefore never matches, so a PATCH left the app reading the
    // pre-update DTO and looked like nothing was saved. Evict by pattern instead,
    // for the current owner and (if it changed) the original owner.
    evictUserDeviceCache(device.getUserId());
    if (originalUserId != null && !Objects.equals(originalUserId, device.getUserId())) {
      evictUserDeviceCache(originalUserId);
    }

    return deviceMapper.deviceToDeviceResponse(device);
  }

  /**
   * Updates fields of a device by its key.
   * Evicts the device's key based cache, the user ID based cache for the requesting user and the
   * global device cache.
   *
   * @param deviceKey the key of the device to update
   * @param req the device update request DTO
   * @param requestingUserId the ID of the currently authenticated user making the request
   * @return the updated device response DTO
   * @throws DeviceNotFoundException if the device is not found
   * @throws AccessDeniedException   if the device does not belong to the requesting user,
   *                                 or if a non-admin attempts to update restricted fields
   */
  @Caching(evict = {
      @CacheEvict(value = "deviceByKeyCache", key = "#deviceKey"),
      @CacheEvict(value = "allDevicesCache", allEntries = true)
  })
  @Transactional
  public DeviceResponse updateDeviceDetails(String deviceKey, DeviceUpdateRequest req,
      UUID requestingUserId)
      throws AccessDeniedException {
    return doUpdateDeviceDetails(deviceKey, req, requestingUserId, null, false);
  }

  /**
   * Updates fields of a device by its key (admin).
   * Evicts the device's key based cache, the new owner's user ID based cache for the device's owner
   * and the global device cache.
   *
   * @param deviceKey the key of the device to update
   * @param req the admin device update request DTO
   * @return the updated device response DTO
   * @throws DeviceNotFoundException if the device is not found
   * @throws AccessDeniedException   if the device does not belong to the requesting user,
   *                                 or if a non-admin attempts to update restricted fields
   */
  @Caching(evict = {
      @CacheEvict(value = "deviceByKeyCache", key = "#deviceKey"),
      @CacheEvict(value = "allDevicesCache", allEntries = true)
  })
  @Transactional
  public DeviceResponse updateDeviceDetailsAdmin(String deviceKey, AdminDeviceUpdateRequest req)
      throws AccessDeniedException {
    return doUpdateDeviceDetails(deviceKey,
        deviceMapper.adminToNonAdminDeviceUpdateRequest(req),
        null, req.userId(), true);
  }

  /**
   * Delete a device by its unique key (Admin only).
   * Evicts the device's key based cache, the user ID based cache for the device's owner and the
   * global device cache.
   *
   * @param deviceKey the key of the device to delete
   * @throws DeviceNotFoundException if the device is not found
   */
  @Transactional
  public void deleteDeviceByKey(String deviceKey) {
    Device device = deviceRepository.findByKey(deviceKey)
        .orElseThrow(() -> new DeviceNotFoundException("Device not found for key: " + deviceKey));
    UUID userId = device.getUserId();
    deviceRepository.delete(device);

    // Evict caches manually
    Cache deviceCache = cacheManager.getCache("deviceByKeyCache");
    if (deviceCache != null) deviceCache.evict(deviceKey);
    if (userId != null) {
      Cache userCache = cacheManager.getCache("deviceByUserIdCache");
      if (userCache != null) userCache.evict(userId);
    }
    Cache allCache = cacheManager.getCache("allDevicesCache");
    if (allCache != null) allCache.clear();
  }

  /**
   * Persists the order of devices for a specific user. The order is determined by the list of
   * device IDs provided. Called when a user hits save and there was a change in the order of their
   * devices in the UI.
   * Evicts the user ID based device cache, the global device cache and the device key based cache.
   *
   * @param userId the ID of the user whose device order is being persisted
   * @param deviceIds the list of device IDs in the desired order
   */
  @Caching(
      evict = {
          @CacheEvict(value = "deviceByUserIdCache", key = "#userId"),
          @CacheEvict(value = "allDevicesCache", allEntries = true),
          @CacheEvict(value = "deviceByKeyCache", allEntries = true)
      }
  )
  @Transactional
  public void persistDeviceOrder(UUID userId, List<Long> deviceIds) {
    List<Device> userDevices = deviceRepository.findAllById(deviceIds);
    Map<Long, Device> deviceMap = userDevices.stream()
        .collect(Collectors.toMap(Device::getId, Function.identity()));

    for (int i = 0; i < deviceIds.size(); i++) {
      Long deviceId = deviceIds.get(i);
      Device device = deviceMap.get(deviceId);
      // Ownership check
      if (device != null && Objects.equals(device.getUserId(), userId)) {
        device.setDisplayOrder((long) (i + 1));
      }
    }

    deviceRepository.saveAll(userDevices);
  }

  /*--------------------------*/
  /* Helper Methods */
  /*--------------------------*/

  /**
   * Calculates the next display order for a user's devices.
   *
   * @param userId the user whose devices are being ordered
   * @return the next display order
   */
  private long calculateDeviceOrder(UUID userId) {
    RestResponsePage<DeviceResponse> devices = deviceCacheService.getDevicesByUserId(userId, Pageable.unpaged());
    return devices.stream()
        .map(DeviceResponse::displayOrder)
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder())
        .map(maxOrder -> maxOrder + 1)
        .orElse(1);
  }

  /**
   * Retrieves a device by its unique key.
   * This method uses caching to reduce database load for frequently accessed device data.
   *
   * @param deviceKey the key of the device to retrieve
   * @return the device entity
   * @throws DeviceNotFoundException if the device is not found
   */
  public Device getDeviceByKey(String deviceKey) {
    return deviceCacheService.getDeviceByKey(deviceKey);
  }

    /**
     * Regenerates a device's secret.
     *
     * <p>Rotation is two-phase and requires the device to be reachable:
     * <ol>
     *   <li>{@code DEVICE_ONLINE}: the device must be powered on, connected to
     *       AWS IoT and subscribed to {@code hydro/{deviceKey}/command}, so
     *       the {@code SetSecret} command below is actually delivered.</li>
     *   <li>{@code DEVICE_ACKS}: the device persists the new secret to EEPROM and
     *       publishes a {@code secret_rotated} status back on
     *       {@code hydro/{deviceKey}/status}. Only after that ack is received and
     *       processed (see {@link #confirmSecretRotation}) is the new secret
     *       written to the database and the pending change cleared.</li>
     * </ol>
     *
     * <p>Until the ack is confirmed the DB keeps the previous secret, so the old
     * secret continues to authenticate. If the device is offline (or cannot ack),
     * the staged change is never committed and times out of
     * {@code pendingSecretChanges} (5 minutes) without taking effect.
     *
     * <p>Optionally, ack can be skipped (for example, if the device is known to be offline) or
     * for testing purposes.</p>
     *
     * @param deviceKey the key of the device for which to regenerate the secret
     * @param requireAck whether to wait for the device to acknowledge before committing
     * @return the new secret in raw form (stored encrypted)
     * @throws DeviceNotFoundException if the device is not found
     */
    @Transactional
    public String regenerateDeviceSecret(String deviceKey, boolean requireAck) {
      requireDeviceByKey(deviceKey);
      String rawSecret = DeviceKeyEncryptionUtil.generateRandomString(32);
      log.debug("Regenerated secret for device {}: {}", deviceKey, rawSecret);

      if (requireAck) {
        pendingSecretChanges.put(deviceKey, rawSecret);
        DeviceCommandRequest command = DeviceCommandRequest.builder().action(DeviceCommandAction.SET_SECRET).cause(
            DeviceCommandCause.MANUAL).secret(rawSecret).build();
        commandGateway.publishCommand(deviceKey, command);
        log.debug("Awaiting device {} to acknowledge secret change", deviceKey);
      } else {
        Device device = requireDeviceByKey(deviceKey);
        device.setSecret(encryptionUtil.encrypt(rawSecret));
        device.setSecretFingerprint(encryptionUtil.fingerprint(rawSecret));
        deviceRepository.save(device);
        pendingSecretChanges.invalidate(deviceKey);
        log.debug("Device {} secret rotated without ack", deviceKey);
      }
      return rawSecret;
    }

  /**
   * Confirms the secret rotation for a device. This method is called after the device acknowledges the secret change.
   *
   * <p>The device echoes back the exact secret it persisted (in the ack's
   * {@code secret} field), and that value is what gets committed. Using the acked
   * secret rather than the in-memory {@code pendingSecretChanges} cache prevents
   * overlapping regenerations or cache expiry from committing the wrong value.
   *
   * <p>Meant to only be called by the {@link #handleSecretRotated(SecretRotatedEvent)}
   * event listener, which handles the transaction.</p>
   *
   * <p>Evicts the device's key based cache, the user ID based cache for the device's owner and the
   * global device cache.</p>
   *
   * @param deviceKey the key of the device for which the secret rotation is being confirmed
   * @param ackPayload the acknowledgment payload received from the device
   */
  public void confirmSecretRotation(String deviceKey, String ackPayload) {
    log.info("Processing secret rotation ack from device {}: {}", deviceKey, ackPayload);
    JsonNode ack;
    ack = objectMapper.readTree(ackPayload);

    if (!"ok".equals(ack.path("status").asString())) {
      log.warn("Device {} reported failed secret write: {}", deviceKey, ackPayload);
      return;
    }

    String ackedSecret = ack.path("secret").asString();
    if (ackedSecret.isEmpty()) {
      log.warn("Secret rotation ack from device {} did not include a secret", deviceKey);
      return;
    }

    // Ignore ack if device key is not in secret rotation pending cache
    String pending = pendingSecretChanges.getIfPresent(deviceKey);
    if (pending == null) {
      log.warn("Secret rotation ack for device {} with no pending rotation; ignoring", deviceKey);
      return;
    }

    Device device = requireDeviceByKey(deviceKey);
    device.setSecret(encryptionUtil.encrypt(ackedSecret));
    device.setSecretFingerprint(encryptionUtil.fingerprint(ackedSecret));
    deviceRepository.save(device);
    pendingSecretChanges.invalidate(deviceKey);

    // Manual cache eviction
    Cache deviceCache = cacheManager.getCache("deviceByKeyCache");
    if (deviceCache != null) deviceCache.evict(deviceKey);
    if (device.getUserId() != null) {
      Cache userCache = cacheManager.getCache("deviceByUserIdCache");
      if (userCache != null) userCache.evict(device.getUserId());
    }
    Cache allCache = cacheManager.getCache("allDevicesCache");
    if (allCache != null) allCache.clear();
    log.debug("Device {} secret rotated successfully", deviceKey);
  }

  @EventListener
  @Transactional
  public void handleSecretRotated(SecretRotatedEvent event) {
    confirmSecretRotation(event.getDeviceKey(), event.getAckPayload());
  }

  /**
   * Dispatches the OTA update command to devices matching the update's technical name.
   *
   * <p>A fresh presigned URL is minted from the stored object key on every dispatch so
   * expired URLs don't strand devices. Devices whose firmware is already at least as
   * new as the published version are skipped (semver-ish compare; firmware is a
   * free-form string, not a double).
   *
   * <p>Used both by the upload-triggered listener and by the scheduled re-dispatch
   * that catches devices which were offline when the firmware was first published.
   *
   * @param update the persisted OTA update to dispatch
   */
  @Transactional
  public void dispatchOtaUpdate(OtaUpdateRecord update) {
    if (update.objectKey() == null) {
      log.warn("OTA update {} v{} has no object key; nothing to dispatch", update.technicalName(), update.version());
      return;
    }

    log.info("Dispatching OTA update: {} v{} objectKey={}", update.technicalName(), update.version(), update.objectKey());
    String binUrl = otaUpdateService.generatePresignedUrl(update.objectKey());

    // Get all devices with matching technical name
    List<Device> devices = deviceRepository.findAllByTechnicalName(update.technicalName());
    if (devices.isEmpty()) {
      log.info("No devices found with technical name {} for OTA update", update.technicalName());
      return;
    }

    List<Device> toPersist = new ArrayList<>();
    for (Device device : devices) {
      // Skip if already seen this update
      if (device.getLastOtaUpdateId() != null && device.getLastOtaUpdateId() >= update.id()) {
        log.debug("Update {} already dispatched to device {}; skipping", update.id(), device.getKey());
        continue;
      }
      // Skip if firmware already up-to-date (unless forceInstall)
      if (!update.forceInstall()
          && device.getFirmware() != null
          && FirmwareVersionComparator.compare(device.getFirmware(), update.version()) >= 0) {
        log.info("Device version is already up to date ({} >= {}), skipping OTA update for device {}",
            device.getFirmware(), update.version(), device.getKey());
        continue;
      }

      // Announce the update to the device on its announce topic
      log.info("Announcing OTA update to device {}", device.getKey());
      commandGateway.publishAnnounce(device.getKey(),
          """
          {"action":"OTAUpdate","cause":"Manual","binUrl":"%s","version":"%s","sha256":"%s"}
          """.formatted(binUrl, update.version(), update.sha256()),
          true);

      device.setLastOtaUpdateId(update.id());
      toPersist.add(device);
    }

    if (toPersist.isEmpty()) {
      log.info("No devices needed OTA update notification.");
      return;
    }

    // Save all updated devices
    deviceRepository.saveAll(toPersist);

    // Evict all affected caches
    evictDeviceCaches(toPersist);
  }

  /**
   * Evicts caches for devices that received an OTA update.
   * This includes the device's individual cache, the user's device list cache,
   * and the global admin list cache.
   */
  private void evictDeviceCaches(List<Device> updatedDevices) {
    Cache userCache = cacheManager.getCache("deviceByUserIdCache");
    Cache deviceCache = cacheManager.getCache("deviceByKeyCache");
    Cache allDevicesCache = cacheManager.getCache("allDevicesCache");

    // Evict individual device keys
    if (deviceCache != null) {
      updatedDevices.forEach(device -> deviceCache.evict(device.getKey()));
    }

    // Evict user-specific lists
    if (userCache != null) {
      updatedDevices.stream()
          .map(Device::getUserId)
          .filter(Objects::nonNull)
          .distinct()
          .forEach(userCache::evict);
    }

    // Evict global admin list completely
    if (allDevicesCache != null) {
      allDevicesCache.clear();
    }
  }

  /**
   * Handles a freshly uploaded firmware file. Runs asynchronously after the upload
   * transaction commits so the device announce fan-out never blocks the upload thread.
   */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleOTAFileUploaded(OTAFileUploadEvent event) {
    log.info("OTA file uploaded: {} v{} forceInstall={}",
        event.getTechnicalName(), event.getFirmwareVersion(), event.isForceInstall());
    otaUpdateService.getLatestByTechnicalName(event.getTechnicalName())
        .ifPresentOrElse(
            this::dispatchOtaUpdate,
            () -> log.warn("No persisted OTA update found for {}; nothing to dispatch", event.getTechnicalName()));
  }

  /**
   * Retrieves the secret for a device by its key. The secret is decrypted before being returned.
   *
   * @param deviceKey the key of the device
   * @return the device's secret (decrypted)
   * @throws DeviceNotFoundException if the device is not found
   */
  public String getSecretByDeviceKey(String deviceKey) {
    Device device = deviceRepository.findByKey(deviceKey)
        .orElseThrow(() -> new DeviceNotFoundException("Device not found for key: " + deviceKey));
    return encryptionUtil.decrypt(device.getSecret());
  }

  /**
   * Topic filters covering all of a user's devices, used to build their IoT subscription scope.
   *
   * @return one wildcard topic filter per device key
   */
  public List<String> getUserDeviceTopics(UUID userId) {
    List<Device> devices = deviceRepository.findAllByUserId(userId, Pageable.unpaged()).getContent();
    return devices.stream()
        .map(device -> "hydro/" + device.getKey() + "/#")
        .toList();
  }

  Device requireDeviceByKey(String deviceKey) {
    return deviceRepository.findByKey(deviceKey)
        .orElseThrow(() -> new DeviceNotFoundException(deviceKey));
  }

  /**
   * Publishes a command to a device after confirming the caller owns it.
   */
  public void sendCommand(String deviceKey, String userSub, DeviceCommandRequest commandRequest) {
    assertOwnership(deviceKey, userSub);
    commandGateway.publishCommand(deviceKey, commandRequest);
  }

  /**
   * Throws if the given user does not own the device.
   */
  public void assertOwnership(String deviceKey, String userSub) {
    if (!deviceRepository.existsByKeyAndUserSub(deviceKey, userSub)) {
      throw new AccessDeniedException("Device not owned by user");
    }
  }

  /**
   * Signs the stored image key into a fresh URL for the response. Rows holding an old
   * full URL are left as-is.
   */
  private DeviceResponse signImageUrl(DeviceResponse response) {
    String imageUrl = response.imageUrl();
    if (imageUrl == null || imageUrl.isBlank() || imageUrl.startsWith("http")) {
      return response;
    }
    try {
      return response.toBuilder()
          .imageUrl(otaUpdateService.generatePresignedUrl(imageUrl))
          .build();
    } catch (Exception e) {
      log.warn("Could not sign image key {} for device {}; returning key as-is",
          imageUrl, response.key(), e);
      return response;
    }
  }
}

/**
 * Service for caching device queries using Spring Cache abstraction. Reduces database load for
 * frequently accessed device data.
 */
@RequiredArgsConstructor
@Service
class DeviceCacheService {

  private final DeviceRepository deviceRepository;
  private final DeviceMapper deviceMapper;

  /**
   * Retrieves a device by its key from cache. Cache is invalidated when the device is updated.
   *
   * @param deviceKey the key of the device to retrieve
   * @return the device if found
   * @throws DeviceNotFoundException if the device is not found
   */
  @Cacheable(
      value = "deviceByKeyCache",
      key = "#deviceKey"
  )
  public Device getDeviceByKey(String deviceKey) {
    return requireDeviceByKey(deviceKey);
  }

  /**
   * Retrieves all devices for a specific user from cache
   *
   * @param userId the ID of the user whose devices are to be retrieved
   * @return list of devices owned by the user
   */
  @Cacheable(value = "deviceByUserIdCache", key = "#userId + '-' + #pageable")
  public RestResponsePage<DeviceResponse> getDevicesByUserId(UUID userId, Pageable pageable) {
    Page<Device> device = deviceRepository.findAllByUserIdWithPins(userId, pageable);
    List<DeviceResponse> deviceResponses = device.stream()
        .map(deviceMapper::deviceToDeviceResponse)
        .toList();
    return new RestResponsePage<>(deviceResponses, pageable, device.getTotalElements());
  }

  /**
   * Retrieves all devices in the system from cache, with pagination
   *
   * @return list of all devices
   */
  @Cacheable(value = "allDevicesCache", key = "#pageable")
  public RestResponsePage<DeviceResponse> getAllDevices(Pageable pageable) {
    Page<Device> devices = deviceRepository.findAllWithPins(pageable);
    List<DeviceResponse> deviceResponses = devices.stream()
        .map(deviceMapper::deviceToDeviceResponse)
        .toList();
    return new RestResponsePage<>(deviceResponses, pageable, devices.getTotalElements());
  }

  private Device requireDeviceByKey(String deviceKey) {
    return deviceRepository.findByKey(deviceKey)
        .orElseThrow(() -> new DeviceNotFoundException(deviceKey));
  }
}