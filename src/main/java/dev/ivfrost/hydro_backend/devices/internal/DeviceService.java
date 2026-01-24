package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.devices.DeviceAuthRequest;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceLoadEvent;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionRequest;
import dev.ivfrost.hydro_backend.devices.DeviceResponse;
import dev.ivfrost.hydro_backend.devices.DeviceUpdateRequest;
import dev.ivfrost.hydro_backend.exception.DeviceFetchException;
import dev.ivfrost.hydro_backend.exception.DeviceLinkException;
import dev.ivfrost.hydro_backend.exception.DeviceNotFoundException;
import dev.ivfrost.hydro_backend.exception.DuplicateMacAddressException;
import dev.ivfrost.hydro_backend.tokens.EncryptionUtil;
import dev.ivfrost.hydro_backend.tokens.MqttTokenProvider;
import dev.ivfrost.hydro_backend.users.UserMqttTokenPayload;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceService {

  private final DeviceRepository deviceRepository;
  private final DeviceCacheService deviceCacheService;
  private final MqttTokenProvider mqttTokenProvider;

  /**
   * Provisions a new device and generates a secret for ownership verification.
   *
   * @param req the device provision request DTO
   * @return the provisioned device response DTO
   * @throws DuplicateMacAddressException if a device with the same MAC address already exists
   */
  @CacheEvict(value = "allDevicesCache", allEntries = true)
  @Transactional
  public DeviceResponse provisionDevice(DeviceProvisionRequest req) {
    // Check for duplicate MAC address before attempting save
    if (deviceRepository.existsByMacAddress(req.macAddress())) {
      throw new DuplicateMacAddressException(req.macAddress());
    }

    Device device = convertRequestToDevice(req);

    // Generate and encrypt device secret (serves as ownership proof)
    device.setSecret(
        EncryptionUtil.generateRandomString(32)); // Will be auto-encrypted by converter

    return DeviceUtil.convertDeviceToResponse(deviceRepository.save(device));
  }

  /**
   * Links an unlinked device to a user using the device secret as ownership proof
   *
   * @param req the device link request DTO (contains device secret)
   * @throws DeviceLinkException     if the device is already linked
   * @throws DeviceNotFoundException if the device is not found
   */
  @CacheEvict(value = "allDevicesCache", allEntries = true)
  @Transactional
  public void linkDevice(DeviceLinkRequest req, Long userId, boolean unlink) {
    // Get device by ID from request, then verify secret matches
    Device device = deviceRepository.findById(req.deviceId())
        .orElseThrow(() -> new DeviceNotFoundException("Device not found"));

    // Verify the secret matches (converter automatically decrypts)
    if (device.getSecret() == null || !device.getSecret().equals(req.secret())) {
      throw new DeviceNotFoundException("Device secret does not match");
    }

    if (!unlink) {
      // Linking device
      if (device.getUserId() != null) {
        throw new DeviceLinkException("Device is already linked to a user");
      }
      device.setUserId(userId);
      device.setLinkedAt(Instant.now());
      device.setDisplayOrder(calculateDeviceOrder(userId));
      deviceRepository.save(device);
    } else {
      // Unlinking device
      if (device.getUserId() == null || !Objects.equals(device.getUserId(), userId)) {
        throw new DeviceLinkException("Device is not linked to this user");
      }
      device.setUserId(null);
      device.setDisplayOrder(0L);
      deviceRepository.save(device);
    }
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
    Device device = deviceRepository.findById(deviceId)
        .orElseThrow(() -> new DeviceNotFoundException(deviceId));
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
    List<Device> devices = deviceRepository.findAllByUserId(userId);
    log.debug("Fetched {} devices for user ID {}", devices.size(), userId);

    if (devices.isEmpty()) {
      throw new DeviceFetchException("No devices found for user");
    }
    return devices
        .stream()
        .map(DeviceUtil::convertDeviceToResponse)
        .sorted(Comparator.comparing(DeviceResponse::order))
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
  public DeviceResponse updateDeviceFriendlyName(DeviceUpdateRequest req) {
    Long deviceId = req.id();
    Device device = deviceRepository.findById(deviceId)
        .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    verifyDeviceOwnership(req.userId(), deviceId);
    device.setFriendlyName(req.friendlyName());
    return DeviceUtil.convertDeviceToResponse(deviceRepository.save(device));
  }


  /**
   * Updates fields of a specific device by its ID.
   *
   * @param req the device update request DTO
   * @return the updated device response DTO
   * @throws DeviceNotFoundException  if the device is not found
   * @throws IllegalArgumentException if the device does not belong to the user
   */
  public DeviceResponse updateDeviceDetails(DeviceUpdateRequest req) {
    Long deviceId = req.id();
    Device device = deviceRepository.findById(deviceId).orElseThrow(
        () -> new DeviceNotFoundException(deviceId));

    // Verify ownership
    if (!Objects.equals(device.getUserId(), req.userId())) {
      throw new IllegalArgumentException("Device does not belong to the user");
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

    return DeviceUtil.convertDeviceToResponse(deviceRepository.save(device));
  }

  /**
   * Delete a device by its ID (Admin only).
   *
   * @param deviceId the ID of the device to delete
   * @throws DeviceNotFoundException if the device is not found
   */
  public void deleteDeviceById(Long deviceId) {
    Device device = deviceRepository.findById(deviceId)
        .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    deviceRepository.delete(device);
  }

  /**
   * Persists the device order stored in Redis to the main database for the authenticated user.
   */
  @Transactional
  public void saveDeviceOrder(DeviceUpdateRequest req) {
    Device device = deviceRepository.findById(req.id())
        .orElseThrow(() -> new DeviceNotFoundException(req.id()));
    verifyDeviceOwnership(req.userId(), req.id());
    device.setDisplayOrder(req.displayOrder());
    deviceRepository.save(device);
  }

  /**
   * Authenticates a device and returns an MQTT JWT token. Device publishes to the topic:
   * hydro/{device-secret}/*
   */
  public String getMqttAuthToken(DeviceAuthRequest req) {
    // Load device by ID and verify secret matches
    Device device = deviceRepository.findById(req.deviceId())
        .orElseThrow(() -> new DeviceNotFoundException("Device not found"));

    // Verify the secret matches (converter automatically decrypts)
    if (device.getSecret() == null || !device.getSecret().equals(req.secret())) {
      throw new DeviceNotFoundException("Device secret does not match");
    }

    // Build topics list containing only this device's publish topic
    List<String> topics = List.of("hydro/" + device.getSecret() + "/#");

    log.debug("Generating MQTT token for device {}, topics: {}", device.getId(), topics);
    return mqttTokenProvider.generateMqttToken(
        new UserMqttTokenPayload(device.getId(), topics)
    );
  }

  /**
   * Authenticates a device (using DeviceLinkRequest) and returns an MQTT JWT token.
   */
  public String getMqttAuthToken(DeviceLinkRequest req) {
    return getMqttAuthToken(new DeviceAuthRequest(req.deviceId(), req.secret()));
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
   * @return the new secret (decrypted)
   * @throws DeviceNotFoundException if the device is not found
   */
  @Transactional
  public String regenerateDeviceSecret(Long deviceId) {
    Device device = getDeviceById(deviceId);
    String newSecret = EncryptionUtil.generateRandomString(32);
    device.setSecret(newSecret); // Will be auto-encrypted by converter
    deviceRepository.save(device);
    return newSecret; // Return decrypted secret
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
    return device.getSecret();
  }

  /**
   * Builds the list of MQTT topics the current user can access based on their devices.
   *
   * @return the list of MQTT topics
   */
  public List<String> getUserDeviceTopics(Long userId) {
    List<Device> devices = deviceRepository.findAllByUserId(userId);
    return devices.stream()
        .map(device -> "hydro/" + device.getSecret() + "/#")
        .toList();
  }

  @ApplicationModuleListener
  public void on(DeviceLoadEvent e) {
    getDevicesByUserId(e.userId());
    log.info("Loaded devices for user ID {} into cache", e.userId());
  }
}


/**
 * Service for caching device queries using Spring Cache abstraction. Reduces database load for
 * frequently accessed device data.
 */
@Slf4j
@AllArgsConstructor
@Service
class DeviceCacheService {

  private final DeviceRepository deviceRepository;

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

}