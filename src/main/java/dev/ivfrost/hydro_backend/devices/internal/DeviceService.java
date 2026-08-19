package dev.ivfrost.hydro_backend.devices.internal;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.RemovalCause;
import dev.ivfrost.hydro_backend.common.RestResponsePage;
import dev.ivfrost.hydro_backend.config.DeviceProperties;
import dev.ivfrost.hydro_backend.config.MqttGateway;
import dev.ivfrost.hydro_backend.config.SecretRotatedEvent;
import dev.ivfrost.hydro_backend.devices.AdminDeviceUpdateRequest;
import dev.ivfrost.hydro_backend.devices.DeviceAuthRequest;
import dev.ivfrost.hydro_backend.devices.DeviceFetchException;
import dev.ivfrost.hydro_backend.devices.DeviceLinkException;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceMapper;
import dev.ivfrost.hydro_backend.devices.DeviceNotFoundException;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionRequest;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionResponse;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.MqttAclRequest;
import dev.ivfrost.hydro_backend.devices.MqttAuthRequest;
import dev.ivfrost.hydro_backend.devices.DeviceUpdateRequest;
import dev.ivfrost.hydro_backend.devices.DuplicateMacAddressException;
import dev.ivfrost.hydro_backend.tokens.DeviceTokenProvider;
import dev.ivfrost.hydro_backend.tokens.DeviceKeyEncriptionUtil;
import dev.ivfrost.hydro_backend.tokens.MqttTokenPayload;
import dev.ivfrost.hydro_backend.tokens.TokenResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceService {

  private final DeviceRepository deviceRepository;
  private final DeviceCacheService deviceCacheService;
  private final DeviceTokenProvider deviceTokenProvider;
  private final DeviceKeyEncriptionUtil encryptionUtil;
  private final CacheManager cacheManager;
  private final DeviceMapper deviceMapper;
  private final ObjectMapper objectMapper;
  private final OtaUpdateService otaUpdateService;

  private final DeviceProperties deviceProperties;
  private final MqttGateway mqttGateway;
  private final com.github.benmanes.caffeine.cache.Cache<String, String> pendingSecretChanges = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMinutes(5))
      .removalListener((key, value, cause) -> {
        if (cause == RemovalCause.EXPIRED) {
          log.warn("Secret rotation timed out for device: {}", key);
        }
      })
      .build();

  /**
   * Provisions a new device and generates a secret for ownership verification.
   * Evicts the global device cache.
   * Meant to be called by an admin user through the API.
   *
   * @param req the device provision request DTO
   * @return the provisioned device response DTO
   * @throws DuplicateMacAddressException if a device with the same MAC address already exists
   */
  @Transactional
  @Caching(evict = {
      @CacheEvict(value = "allDevicesCache", allEntries = true),
  })
  public DeviceProvisionResponse provisionDevice(DeviceProvisionRequest req) {

    if (deviceRepository.existsByMacAddress(req.macAddress())) {
      throw new DuplicateMacAddressException(req.macAddress());
    }

    Device device = deviceMapper.deviceProvisionRequestToDevice(req);

    // Generate, hash and set device secret
    String rawSecret = DeviceKeyEncriptionUtil.generateRandomString(32);
    String hashed = encryptionUtil.encrypt(rawSecret);
    device.setSecret(hashed);
    Device saved = deviceRepository.save(device);

    // Return device details along with the raw secret
    return deviceMapper.deviceToDeviceProvisionResponse(saved, rawSecret);
  }

  /**
   * Provisions a new device and generates a secret for ownership verification.
   * Meant to be called by post build hook by the ESP32 device itself.
   *
   * @param req the device provision request DTO
   * @param authorizationHeader the authorization header containing the provisioning token
   * @return the provisioned device response DTO
   */
  @Transactional
  @Caching(evict = {
      @CacheEvict(value = "allDevicesCache", allEntries = true),
      @CacheEvict(value = "deviceByKeyCache", allEntries = true)
  })
  public DeviceProvisionResponse provisionDevice(DeviceProvisionRequest req, String authorizationHeader) {
    log.debug("authorizationHeader raw = '{}'", authorizationHeader);
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new BadCredentialsException("Missing or invalid Authorization header");
    }
    String token = authorizationHeader.replace("Bearer ", "").trim();
    String provisioningSecret = deviceProperties.provisioningSecret();
    log.debug("token='{}' (len={}), provisioningSecret='{}' (len={})",
        token, token.length(), provisioningSecret, provisioningSecret.length());
    if (!provisioningSecret.equals(token)) {
      throw new BadCredentialsException("Invalid provisioning token");
    }

    Device device = deviceMapper.deviceProvisionRequestToDevice(req);

    // Generate, hash and set device secret
    String rawSecret = DeviceKeyEncriptionUtil.generateRandomString(32);
    String hashed = encryptionUtil.encrypt(rawSecret);
    device.setSecret(hashed);

    // Save device
    Device saved = deviceRepository.upsert(device);

    // Return device details along with the raw secret
    return deviceMapper.deviceToDeviceProvisionResponse(saved, rawSecret);
  }

  /**
   * Links an unlinked device to a user using the device secret as ownership proof
   * Evicts the user ID based device cache.
   *
   * @param req the device link request DTO (contains device secret)
   * @return the updated device response DTO after linking
   * @throws DeviceLinkException     if the device is already linked
   * @throws DeviceNotFoundException if the device is not found
   */
  @CacheEvict(value = "deviceByUserIdCache", key = "#userId")
  @Transactional
  public DeviceResponse linkDevice(DeviceLinkRequest req, Long userId) {

    // Fetch unlinked device by secret hash
    String encryptedInput = encryptionUtil.encrypt(req.secret());
    Device device = deviceRepository.findBySecret(encryptedInput)
        .orElseThrow(() -> new DeviceNotFoundException("Device not found"));

    if (device.getUserId() != null) {
      throw new DeviceLinkException("Device is already linked to a user");
    }

    device.setUserId(userId);
    device.setLinkedAt(Instant.now());
    device.setDisplayOrder(calculateDeviceOrder(userId));
    deviceRepository.save(device);
    return deviceMapper.deviceToDeviceResponse(device);
  }

  /**
   * Unlinks a device from a user by device key. The device will no longer be associated with the
   * user and will be available for linking by another user or themselves in the future.
   * Evicts the user ID based device cache.
   *
   * @param deviceKey the key of the device to unlink
   * @throws DeviceNotFoundException if the device is not found
   * @throws IllegalArgumentException if the device does not belong to the user
   */
  @CacheEvict(value = "deviceByUserIdCache", key = "#userId")
  @Transactional
  public void unlinkDevice(String deviceKey, Long userId) {
    Device device = deviceRepository.findByKey(deviceKey)
        .orElseThrow(() -> new DeviceNotFoundException("Device not found for key: " + deviceKey));

    if (device.getUserId() == null || !Objects.equals(device.getUserId(), userId)) {
      throw new DeviceLinkException("Device is not linked to this user");
    }

    device.setUserId(null);
    device.setDisplayOrder(0L);
    deviceRepository.save(device);
  }

  /**
   * Verify device ownership
   *
   * @param userId   the user to verify ownership against
   * @param deviceKey the device key to verify ownership of
   * @throws DeviceNotFoundException  if the device is not found
   * @throws IllegalArgumentException if the device does not belong to the specified user
   */
  public void verifyDeviceOwnership(Long userId, String deviceKey) {
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
  public Page<DeviceResponse> getDevicesByUserId(Long userId, Pageable pageable) {
    RestResponsePage<DeviceResponse> devices = deviceCacheService.getDevicesByUserId(userId, pageable);
    log.debug("Fetched {} devices for user ID {}", devices.getContent().size(), userId);
    return devices;
  }

  /**
   * Retrieves all devices provisioned in the system (Admin only, paginated and cached).
   *
   * @return a list of all device response DTOs
   * @throws DeviceFetchException if no devices are found
   */
  public Page<DeviceResponse> getAllDevices(Pageable pageable) {
    return deviceCacheService.getAllDevices(pageable);
  }

  /**
   * Updates fields of a specific device by its ID.
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
      Long requestingUserId, Long newUserId, boolean isAdmin)
      throws AccessDeniedException {
    Device device = requireDeviceByKey(deviceKey);

    String technicalName = req.technicalName();
    String firmware = req.firmware();
    Long originalUserId = device.getUserId();

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

    // Common fields: friendlyName, location, description, imageUrl, displayOrder
    // They can be empty, app will show a fallback, like device key (MQTT) in the case of
    // missing friendly name.
    deviceMapper.updateDeviceFromRequest(req, device);

    // Manually evict old owner cache if userId changed
    if (originalUserId != null && !Objects.equals(originalUserId, device.getUserId())) {
      Cache userCache = cacheManager.getCache("deviceByUserIdCache");
      if (userCache != null) {
        userCache.evict(originalUserId);
      }
    }

    return deviceMapper.deviceToDeviceResponse(device);
  }

  /**
   * Updates fields of a specific device by its ID.
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
      @CacheEvict(value = "deviceByUserIdCache", key = "#requestingUserId"),
      @CacheEvict(value = "allDevicesCache", allEntries = true)
  })
  @Transactional
  public DeviceResponse updateDeviceDetails(String deviceKey, DeviceUpdateRequest req,
      Long requestingUserId)
      throws AccessDeniedException {
    return doUpdateDeviceDetails(deviceKey, req, requestingUserId, null, false);
  }

  /**
   * Updates fields of a specific device by its ID.
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
      @CacheEvict(value = "deviceByUserIdCache", key = "#req.userId()", condition = "#req.userId() != null"),
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
    Long userId = device.getUserId();
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
  public void persistDeviceOrder(Long userId, List<Long> deviceIds) {
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

  public void verifyMqttConnection(MqttAuthRequest req) throws JWTVerificationException {
    deviceTokenProvider.validateMqttToken(req.password());
  }

  public boolean verifyMqttAcl(MqttAclRequest req) throws JWTVerificationException {
    return deviceTokenProvider.validateMqttAcl(req.password(), req.topic(), req.action());
  }

  public TokenResponse authenticateDevice(DeviceAuthRequest req) {
    // Load device by ID and verify secret matches
    Device device = deviceRepository.findByKey(req.key())
        .orElseThrow(() -> new DeviceNotFoundException("Device not found"));

    // Decrypt and compare stored secret hash with the provided raw secret
    String decryptedSecret = encryptionUtil.decrypt(device.getSecret());
    if (!Objects.equals(decryptedSecret, req.secret())) {
      throw new BadCredentialsException("Invalid credentials");
    }

    // Generate MQTT token with topic rules based on device ownership
    // Fall back to -1 for user ID in topic if device is not linked to any user
    Long userId = device.getUserId();
    var deviceUserId = (userId != null && userId != 0) ? userId : -1L;

    return deviceTokenProvider.generateMqttToken(
        new MqttTokenPayload(
            deviceUserId,
            device.getId(),
            List.of("hydro/" + device.getKey() + "/#")
        )
    );
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
  private long calculateDeviceOrder(Long userId) {
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
   *       the MQTT broker and subscribed to {@code hydro/{deviceKey}/command}, so
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
   * @param deviceKey the key of the device for which to regenerate the secret
   * @return the new secret in raw form (not hashed)
   * @throws DeviceNotFoundException if the device is not found
   */
  @Transactional
  public String regenerateDeviceSecret(String deviceKey) {
    requireDeviceByKey(deviceKey);
    String rawSecret = DeviceKeyEncriptionUtil.generateRandomString(32);
    pendingSecretChanges.put(deviceKey, rawSecret);
    log.debug("Regenerated secret for device {}: {}", deviceKey, rawSecret);
    mqttGateway.sendToMqtt(
        """
        {"action":"SetSecret","cause":"Manual","secret":"%s"}
        """.formatted(rawSecret),
        "hydro/" + deviceKey + "/command"
    );
    log.debug("Awaiting device {} to acknowledge secret change", deviceKey);
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
    try {
      ack = objectMapper.readTree(ackPayload);
    } catch (JsonProcessingException e) {
      log.warn("Malformed secret rotation ack from device {}: {}", deviceKey, ackPayload);
      return;
    }

    if (!"ok".equals(ack.path("status").asText())) {
      log.warn("Device {} reported failed secret write: {}", deviceKey, ackPayload);
      return;
    }

    String ackedSecret = ack.path("secret").asText();
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
   * new as the published version are skipped (semver-ish comparison — firmware is a
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

      // Announce update via MQTT
      log.info("Announcing OTA update to device {}", device.getKey());
      mqttGateway.sendRetainedToMqtt(
          """
          {"action":"OTAUpdate","cause":"Manual","binUrl":"%s","version":"%s","sha256":"%s"}
          """.formatted(binUrl, update.version(), update.sha256()),
          "hydro/" + device.getKey() + "/announce",
          true
      );

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
   * transaction commits so MQTT fan-out never blocks the upload request thread.
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
   * Builds the list of MQTT topics the current user can access based on their devices.
   *
   * @return the list of MQTT topics
   */
  public List<String> getUserDeviceTopics(Long userId) {
    List<Device> devices = deviceRepository.findAllByUserId(userId, Pageable.unpaged()).getContent();
    return devices.stream()
        .map(device -> "hydro/" + device.getKey() + "/#")
        .toList();
  }

  private Device requireDeviceByKey(String deviceKey) {
    return deviceRepository.findByKey(deviceKey)
        .orElseThrow(() -> new DeviceNotFoundException(deviceKey));
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
  public RestResponsePage<DeviceResponse> getDevicesByUserId(Long userId, Pageable pageable) {
    Page<Device> device = deviceRepository.findAllByUserId(userId, pageable);
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
    Page<Device> devices = deviceRepository.findAll(pageable);
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