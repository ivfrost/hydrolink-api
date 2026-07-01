package dev.ivfrost.hydro_backend.devices.internal;

import com.auth0.jwt.exceptions.JWTVerificationException;
import dev.ivfrost.hydro_backend.devices.DeviceAuthRequest;
import dev.ivfrost.hydro_backend.devices.DeviceFetchException;
import dev.ivfrost.hydro_backend.devices.DeviceLinkException;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceLoadEvent;
import dev.ivfrost.hydro_backend.devices.DeviceNotFoundException;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionRequest;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionResponse;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.internal.DeviceController.MqttAclRequest;
import dev.ivfrost.hydro_backend.devices.internal.DeviceController.MqttAuthRequest;
import dev.ivfrost.hydro_backend.devices.DeviceUpdateRequest;
import dev.ivfrost.hydro_backend.devices.DuplicateMacAddressException;
import dev.ivfrost.hydro_backend.tokens.DeviceTokenProvider;
import dev.ivfrost.hydro_backend.tokens.EncryptionUtil;
import dev.ivfrost.hydro_backend.tokens.JWTUtil;
import dev.ivfrost.hydro_backend.tokens.MqttTokenPayload;
import dev.ivfrost.hydro_backend.tokens.TokenResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceService {

  private final DeviceRepository deviceRepository;
  private final DeviceCacheService deviceCacheService;
  private final DeviceTokenProvider deviceTokenProvider;
  private final RedisTemplate<String, String> redisTemplate;
  private final JWTUtil jWTUtil;
  private final EncryptionUtil encryptionUtil;
  private final CacheManager cacheManager;

  /**
   * Provisions a new device and generates a secret for ownership verification.
   *
   * @param req the device provision request DTO
   * @return the provisioned device response DTO
   * @throws DuplicateMacAddressException if a device with the same MAC address already exists
   */
  @Transactional
  public DeviceProvisionResponse provisionDevice(DeviceProvisionRequest req) {

    if (deviceRepository.existsByMacAddress(req.macAddress())) {
      throw new DuplicateMacAddressException(req.macAddress());
    }

    Device device = convertRequestToDevice(req);

    // Generate, hash and set device secret
    String rawSecret = EncryptionUtil.generateRandomString(32);
    String hashed = encryptionUtil.encrypt(rawSecret);
    device.setSecret(hashed);

    // Save device
    Device saved = deviceRepository.save(device);

    evictGlobalCache();
    // Return device details along with the raw secret
    return DeviceUtil.convertProvisionDeviceToResponse(saved, rawSecret);
  }

  /**
   * Links an unlinked device to a user using the device secret as ownership proof
   *
   * @param req the device link request DTO (contains device secret)
   * @return the updated device response DTO after linking
   * @throws DeviceLinkException     if the device is already linked
   * @throws DeviceNotFoundException if the device is not found
   */
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
    evictDeviceCaches(device.getId(), userId);
    return DeviceUtil.convertDeviceToResponse(device);
  }

  /**
   * Unlinks a device from a user by device ID. Only the owner can unlink their device.
   *
   * @param deviceId the ID of the device to unlink
   * @throws DeviceNotFoundException if the device is not found
   * @throws IllegalArgumentException if the device does not belong to the user
   */
  @Transactional
  public void unlinkDevice(Long deviceId, Long userId) {
    Device device = deviceRepository.findById(deviceId)
        .orElseThrow(() -> new DeviceNotFoundException(deviceId));

    if (device.getUserId() == null || !Objects.equals(device.getUserId(), userId)) {
      throw new DeviceLinkException("Device is not linked to this user");
    }

    device.setUserId(null);
    device.setDisplayOrder(0L);
    deviceRepository.save(device);
    evictDeviceCaches(deviceId, userId);
  }

  /**
   * Verify device ownership
   *
   * @param userId   the user to verify ownership against
   * @param deviceId the ID of the device to verify
   * @throws DeviceNotFoundException  if the device is not found
   * @throws IllegalArgumentException if the device does not belong to the specified user
   */
  public void verifyDeviceOwnership(Long userId, Long deviceId) {
    Device device = getDeviceById(deviceId);
    if (!Objects.equals(device.getUserId(), userId)) {
      throw new IllegalArgumentException("Device does not belong to the specified user");
    }
  }

  /**
   * Retrieves devices owned by a specific user, by user ID (Admin only).
   *
   * @param userId the ID of the user whose devices are to be retrieved
   * @return a list of device response DTOs
   * @throws DeviceFetchException if no devices are found for the user
   */
  public List<DeviceResponse> getDevicesByUserId(Long userId) {
    List<Device> devices = deviceCacheService.getDevicesByUserId(userId);
    log.debug("Fetched {} devices for user ID {}", devices.size(), userId);

    if (devices.isEmpty()) {
      throw new DeviceFetchException("No devices found for user");
    }
    return devices
        .stream()
        .map(DeviceUtil::convertDeviceToResponse)
        .sorted(Comparator.comparing(DeviceResponse::displayOrder))
        .toList();
  }

  /**
   * Retrieves all devices provisioned in the system (Admin only, paginated).
   *
   * @return a list of all device response DTOs
   * @throws DeviceFetchException if no devices are found
   */
  public Page<DeviceResponse> getAllDevices(Pageable pageable) {
    Page<Device> devices = deviceCacheService.getAllDevices(pageable);
    if (devices.isEmpty()) {
      throw new DeviceFetchException("No devices found in the system");
    }
    return devices.map(DeviceUtil::convertDeviceToResponse);
  }

  /**
   * Updates the friendly friendlyName of a specific device by its ID.
   */
  @Transactional
  public DeviceResponse updateDeviceFriendlyName(long deviceId, DeviceUpdateRequest req) {
    Device device = deviceRepository.findById(deviceId)
        .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    verifyDeviceOwnership(req.userId(), deviceId);
    device.setFriendlyName(req.friendlyName());

    Device saved = deviceRepository.save(device);
    evictDeviceCaches(deviceId, req.userId());
    return DeviceUtil.convertDeviceToResponse(saved);
  }

  @Transactional
  public DeviceResponse updateDeviceDetails(long deviceId, DeviceUpdateRequest req, long requestingUserId, boolean isAdmin)
      throws AccessDeniedException {
    return doUpdateDeviceDetails(deviceId, req, requestingUserId, isAdmin);
  }

  /**
   * Update devices override for admin users
   */
  @Transactional
  public DeviceResponse updateDeviceDetailsAdmin(long deviceId, DeviceUpdateRequest req) {
    return doUpdateDeviceDetails(deviceId, req, 0L, true);
  }

  /**
   * Updates fields of a specific device by its ID.
   *
   * @param deviceId the ID of the device to update
   * @param req the device update request DTO
   * @param requestingUserId the ID of the currently authenticated user making the request
   * @param isAdmin whether the request is made by an admin user
   * @return the updated device response DTO
   * @throws DeviceNotFoundException if the device is not found
   * @throws AccessDeniedException   if the device does not belong to the requesting user,
   *                                 or if a non-admin attempts to update restricted fields
   */
  private DeviceResponse doUpdateDeviceDetails(long deviceId, DeviceUpdateRequest req, long requestingUserId, boolean isAdmin)
      throws AccessDeniedException {
    Device device = deviceRepository.findById(deviceId).orElseThrow(
        () -> new DeviceNotFoundException(deviceId));

    // Verify ownership and guard against non-admin users trying to update restricted fields
    if (!isAdmin) {
      verifyDeviceOwnership(requestingUserId, deviceId);
      if (req.technicalName() != null || req.firmware() != null || req.userId() != null) {
        throw new AccessDeniedException("Non-admin users cannot update technicalName, firmware, or userId");
      }
    }

    String technicalName = req.technicalName();
    String firmware = req.firmware();
    String name = req.friendlyName();

    if (technicalName != null && !technicalName.isEmpty()) {
      device.setTechnicalName(technicalName);
    }
    if (firmware != null && !firmware.isEmpty()) {
      device.setFirmware(firmware);
    }
    if (name != null && !name.isEmpty()) {
      device.setFriendlyName(name);
    }
    if (isAdmin && req.userId() != null) {
      device.setUserId(req.userId());
    }
    if (req.displayOrder() != null && req.displayOrder() > 0) {
      device.setDisplayOrder(req.displayOrder());
    }

    Device saved = deviceRepository.save(device);
    evictDeviceCaches(deviceId, isAdmin ? saved.getUserId() : requestingUserId);
    return DeviceUtil.convertDeviceToResponse(saved);
  }

  /**
   * Delete a device by its ID (Admin only).
   *
   * @param deviceId the ID of the device to delete
   * @throws DeviceNotFoundException if the device is not found
   */
  @Transactional
  public void deleteDeviceById(Long deviceId) {
    Device device = deviceRepository.findById(deviceId)
        .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    Long ownerId = device.getUserId();
    deviceRepository.delete(device);
    evictDeviceCaches(deviceId, ownerId);
  }

  /**
   * Updates the last seen timestamp of a device to the current time. This should be called whenever
   * the device interacts with the system (e.g., on MQTT connection, API request, etc.) to keep
   * track of active devices.
   *
   * @param deviceId the ID of the device to update
   * @throws DeviceNotFoundException if the device is not found
   */
  @Transactional
  public void persistLastSeen(Long deviceId) {
    Device device = deviceRepository.findById(deviceId)
        .orElseThrow(() -> new DeviceNotFoundException(deviceId));

    device.setLastSeen(Instant.now());
    deviceRepository.save(device);
  }

  /**
   * Updates the live order of devices for a user in Redis. This is used to maintain the display
   * order of devices in the UI without hitting the database on every change.
   *
   * @param deviceIds the list of device IDs in the new order
   */
  public void updateLiveOrder(Long userId, List<Long> deviceIds) {
    String key = "device_order:" + userId;

    // Clear existing list
    redisTemplate.delete(key);

    // Push new order
    redisTemplate.opsForList().rightPushAll(
        key,
        deviceIds.stream().map(String::valueOf).toList()
    );
  }

  /**
   * Persists the live device order from Redis to the database. This should be called when the user
   * explicitly saves their device order or when they abandon the tab/page to ensure the order is
   * not lost.
   *
   * @param userId the ID of the user whose device order is being persisted
   */
  @Transactional
  public void persistDeviceOrder(Long userId) {
    String key = "device_order:" + userId;
    List<String> deviceIdStrings = redisTemplate.opsForList().range(key, 0, -1);
    if (deviceIdStrings == null || deviceIdStrings.isEmpty()) {
      return; // No order to persist
    }

    List<Long> deviceIds = deviceIdStrings.stream()
        .map(Long::valueOf)
        .toList();

    List<Device> userDevices = deviceRepository.findAllById(deviceIds);
    Map<Long, Device> deviceMap = userDevices.stream()
        .collect(Collectors.toMap(Device::getId, Function.identity()));

    for (int i = 0; i < deviceIds.size(); i++) {
      Long deviceId = deviceIds.get(i);
      Device device = deviceMap.get(deviceId);
      if (device != null && Objects.equals(device.getUserId(), userId)) {
        device.setDisplayOrder((long) (i + 1));
      }
    }

    deviceRepository.saveAll(userDevices);
    evictDeviceCaches(null, userId);
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
            List.of("hydro/" + "+" + "/" + device.getKey() + "/#")
        )
    );
  }

  public void updateLastSeen(String deviceKey) {
    boolean shouldPersist = deviceCacheService.updateLastSeen(deviceKey);
    if (shouldPersist) {
      Device device = deviceRepository.findByKey(deviceKey)
          .orElseThrow(() -> new DeviceNotFoundException("Device not found for key: " + deviceKey));
      device.setLastSeen(Instant.now());
      deviceRepository.save(device);
    }
  }

  /*--------------------------*/
  /* Helper Methods */
  /*--------------------------*/

  /**
   * Converts a DeviceProvisionRequest DTO to a Device entity.
   *
   * @param req the device provision request DTO
   * @return the device entity
   */
  private Device convertRequestToDevice(DeviceProvisionRequest req) {
    Device device = new Device();
    device.setTechnicalName(req.technicalName());
    device.setFirmware(req.firmware());
    device.setMacAddress(req.macAddress());
    return device;
  }

  /**
   * Converts a Device entity to a DeviceResponse DTO.
   *
   * @param device the device entity
   * @return the device response DTO
   */
  private DeviceResponse convertDeviceToResponse(Device device) {
    return DeviceResponse.builder()
        .id(device.getId())
        .key(device.getKey())
        .technicalName(device.getTechnicalName())
        .friendlyName(device.getFriendlyName())
        .firmware(device.getFirmware())
        .macAddress(device.getMacAddress())
        .userId(device.getUserId())
        .linkedAt(device.getLinkedAt())
        .lastSeen(device.getLastSeen())
        .displayOrder(device.getDisplayOrder())
        .build();
  }

  /**
   * Calculates the next display order for a user's devices.
   *
   * @param userId the user whose devices are being ordered
   * @return the next display order
   */
  private long calculateDeviceOrder(Long userId) {
    List<Device> devices = deviceCacheService.getDevicesByUserId(userId);
    return devices.stream()
        .map(Device::getDisplayOrder)
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder())
        .map(maxOrder -> maxOrder + 1)
        .orElse(1L);
  }

  /**
   * Retrieves a device by its ID.
   *
   * @param deviceId the ID of the device to retrieve
   * @return the device entity
   * @throws DeviceNotFoundException if the device is not found
   */
  public Device getDeviceById(Long deviceId) {
    return deviceCacheService.getDeviceById(deviceId);
  }

  /**
   * Regenerates a device's secret.
   *
   * @param deviceId the ID of the device
   * @return the new secret in raw form (not hashed)
   * @throws DeviceNotFoundException if the device is not found
   */
  @Transactional
  public String regenerateDeviceSecret(Long deviceId) {
    Device device = deviceRepository.findById(deviceId)
        .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    String rawSecret = EncryptionUtil.generateRandomString(32);
    String encryptedSecret = encryptionUtil.encrypt(rawSecret);
    device.setSecret(encryptedSecret);
    deviceRepository.save(device);
    evictDeviceCaches(deviceId, device.getUserId());
    return rawSecret;
  }

  /**
   * Retrieves the secret for a device by its ID (decrypted). The secret is automatically decrypted
   * by the JPA converter.
   *
   * @param deviceId the ID of the device
   * @return the device's secret (decrypted)
   * @throws DeviceNotFoundException if the device is not found
   */
  public String getSecretByDeviceId(Long deviceId) {
    Device device = getDeviceById(deviceId);
    return encryptionUtil.decrypt(device.getSecret());
  }

  /**
   * Builds the list of MQTT topics the current user can access based on their devices.
   *
   * @return the list of MQTT topics
   */
  public List<String> getUserDeviceTopics(Long userId) {
    List<Device> devices = deviceRepository.findAllByUserId(userId);
    return devices.stream()
        .map(device -> "hydro/" + userId + "/" + device.getKey() + "/#")
        .toList();
  }

  @ApplicationModuleListener
  public void on(DeviceLoadEvent e) {
    getDevicesByUserId(e.userId());
    log.info("Loaded devices for user ID {} into cache", e.userId());
  }

  private void evictDeviceCaches(Long deviceId, Long userId) {
    if (deviceId != null) {
      Objects.requireNonNull(cacheManager.getCache("deviceByIdCache")).evict(deviceId);
    }
    if (userId != null) {
      Objects.requireNonNull(cacheManager.getCache("devicesByUserIdCache")).evict(userId);
    }
    evictGlobalCache();
  }

  private void evictGlobalCache() {
    Objects.requireNonNull(cacheManager.getCache("allDevicesCache")).clear();
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
  private final RedisTemplate<String, String> redisTemplate;
  @Value("${cache.last-seen.expiration.ms}")
  private String lastSeenExpirationMs;

  /**
   * Retrieves a device by its ID from cache. Cache is invalidated when the device is updated.
   *
   * @param deviceId the ID of the device to retrieve
   * @return the device if found
   * @throws DeviceNotFoundException if the device is not found
   */
  @Cacheable(
      value = "deviceByIdCache",
      key = "#deviceId"
  )
  public Device getDeviceById(Long deviceId) {
    return deviceRepository.findById(deviceId)
        .orElseThrow(() -> new DeviceNotFoundException(deviceId));
  }

  /**
   * Retrieves all devices for a specific user from cache
   *
   * @param userId the ID of the user whose devices are to be retrieved
   * @return list of devices owned by the user
   */
  @Cacheable(
      value = "devicesByUserIdCache",
      key = "#userId"
  )
  public List<Device> getDevicesByUserId(Long userId) {
    return deviceRepository.findAllByUserId(userId);
  }

  /**
   * Retrieves all devices in the system from cache, with pagination
   *
   * @return list of all devices
   */
  @Cacheable(
      value = "allDevicesCache",
      key = "#pageable.pageNumber + '-' + #pageable.pageSize"
  )
  public Page<Device> getAllDevices(Pageable pageable) {
    return deviceRepository.findAll(pageable);
  }

  /**
   * Updates the last seen timestamp of a device in Redis.
   * Flushes to DB if the last saved
   *
   */
  public boolean updateLastSeen(String deviceKey) {
    Instant now = Instant.now();
    String redisKey = "device_last_seen:" + deviceKey;
    String cachedValue = redisTemplate.opsForValue().get(redisKey);

    // If no cached value exists, set cache and persist to DB immediately
    if (cachedValue == null) {
      redisTemplate.opsForValue().set(redisKey, now.toString());
      return true;
    }

    Instant lastCached = Instant.parse(cachedValue);
    // If cached value, check if expiration time has passed since last persisted value
    boolean shouldPersist = Duration.between(lastCached, now).toMillis() >= Long.parseLong(lastSeenExpirationMs);

    if (shouldPersist) {
      // Always update cache with current timestamp
      redisTemplate.opsForValue().set(redisKey, now.toString());
    }

    return shouldPersist;
  }
}