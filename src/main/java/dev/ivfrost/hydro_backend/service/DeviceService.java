// language: java
package dev.ivfrost.hydro_backend.service;

import dev.ivfrost.hydro_backend.dto.*;
import dev.ivfrost.hydro_backend.entity.Device;
import dev.ivfrost.hydro_backend.entity.MqttCredentials;
import dev.ivfrost.hydro_backend.entity.User;
import dev.ivfrost.hydro_backend.exception.DeviceFetchException;
import dev.ivfrost.hydro_backend.exception.DeviceLinkException;
import dev.ivfrost.hydro_backend.exception.DeviceNotFoundException;
import dev.ivfrost.hydro_backend.repository.DeviceRepository;
import dev.ivfrost.hydro_backend.repository.MqttCredentialsRepository;
import dev.ivfrost.hydro_backend.util.DeviceDtoUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final MqttCredentialsRepository mqttCredentialsRepository;
    private final EncoderService encoderService;
    private final String privateKey;
    private final UserService userService;

    public DeviceService(
            DeviceRepository deviceRepository,
            MqttCredentialsRepository mqttCredentialsRepository,
            EncoderService encoderService,
            @Value("${device.secret}") String privateKey,
            UserService userService) {

        this.deviceRepository = deviceRepository;
        this.mqttCredentialsRepository = mqttCredentialsRepository;
        this.encoderService = encoderService;
        this.privateKey = privateKey;
        if (this.privateKey == null || this.privateKey.isEmpty()) {
            throw new IllegalStateException("Environment variable DEVICE_SECRET is not set.");
        }
        this.userService = userService;
    }

    /**
     * Provisions a new device and generates an ownership hash using the database ID.
     *
     * @param req the device provision request DTO
     * @return the provisioned device response DTO
     */
    @Transactional
    public DeviceResponse provisionDevice(DeviceProvisionRequest req) {
        Device device = convertRequestToDevice(req);

        // Persist to get generated ID
        Device savedDevice = deviceRepository.save(device);

        // Compute and set hash based on generated ID
        String hash = encoderService.hmacSha256Encoder().apply(privateKey, savedDevice.getId().toString());
        savedDevice.setHash(hash);

        // Persist updated record with hash
        return DeviceDtoUtil.convertDeviceToResponse(deviceRepository.save(savedDevice));
    }

    /**
     * Links an unlinked device to a user; creates MQTT credentials if user's first device; sets device order.
     *
     * @param req    the device link request DTO
     * @param userId the ID of the user to link the device to (null for authenticated user)
     * @throws DeviceLinkException     if the device is already linked
     * @throws DeviceNotFoundException if the device is not found
     */
    @Transactional
    public void linkDevice(DeviceLinkRequest req, Long userId) {
        // Determine the user (either from provided ID or current authentication context)
        User user = (userId != null)
                ? userService.getUserByIdWithoutDevices(userId)
                : userService.getCurrentUserWithoutDevices();

        // Find the device by hash
        Device device = deviceRepository.findByHash(req.getHash())
                .orElseThrow(() -> new DeviceNotFoundException(req.getHash()));

        // Check if device is already linked
        if (device.getUser() != null) {
            throw new DeviceLinkException(req.getHash());
        }

        ensureMqttCredentialsForUser(user);

        // Link device to user
        device.setUser(user);
        // Set device order to be the highest order + 1 for initial display at the bottom of the list
        device.setDisplayOrder(calculateDeviceDisplayOrder(user));
        deviceRepository.save(device);
    }

    /**
     * Overloaded method for device linking requests coming from authenticated users.
     *
     * @param req the device link request DTO
     * @throws DeviceLinkException     if the device is already linked
     * @throws DeviceNotFoundException if the device is not found
     */
    @Transactional
    public void linkDevice(DeviceLinkRequest req) {
        linkDevice(req, null);
    }

    /**
     * Unlinks a device from the currently authenticated user.
     *
     * @param deviceId the ID of the device to unlink
     * @throws DeviceNotFoundException  if the device is not found
     * @throws IllegalArgumentException if the device does not belong to the authenticated user
     */
    @Transactional
    public void unlinkDevice(Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
        User currentUser = userService.getCurrentUserWithoutDevices();
        if (!Objects.equals(device.getUser(), currentUser)) {
            throw new IllegalArgumentException("Device does not belong to the authenticated user");
        }
        device.setUser(null);
        deviceRepository.save(device);
    }

    /**
     * Retrieves existing MQTT credentials for the currently authenticated user.
     * The password is returned as stored in the database (hash generated from user ID and private key).
     *
     * @return the MQTT credentials response DTO
     * @throws IllegalArgumentException if credentials are not found for the user
     */
    public MqttCredentialsResponse getMqttCredentials() {
        User user = userService.getCurrentUserWithoutDevices();
        MqttCredentials mqttCredentials = mqttCredentialsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("MQTT credentials not found for user"));
        // Return the password as stored in the database
        return new MqttCredentialsResponse(mqttCredentials.getUsername(), mqttCredentials.getPassword());
    }

    /**
     * Retrieves devices owned by the currently authenticated user.
     *
     * @return a list of device response DTOs
     * @throws DeviceFetchException if no devices are found for the user
     */
    public List<DeviceResponse> getUserDevices() {
        User user = userService.getCurrentUser();

        if (user.getDevices() == null || user.getDevices().isEmpty()) {
            throw new DeviceFetchException("No devices found for user");
        }

        return user.getDevices()
                .stream()
                .map(DeviceDtoUtil::convertDeviceToResponse)
                .sorted(Comparator.comparing(DeviceResponse::getDisplayOrder))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves devices owned by a specific user, by user ID (Admin only).
     *
     * @param userId the ID of the user whose devices are to be retrieved
     * @return a list of device response DTOs
     * @throws DeviceFetchException if no devices are found for the user
     */
    public List<DeviceResponse> getUserDevicesById(Long userId) {
        User user = userService.getUserById(userId);
        if (user.getDevices() == null || user.getDevices().isEmpty()) {
            throw new DeviceFetchException("No devices found for user");
        }
        return user
                .getDevices()
                .stream()
                .map(DeviceDtoUtil::convertDeviceToResponse)
                .sorted(Comparator.comparing(DeviceResponse::getDisplayOrder))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all devices provisioned in the system (Admin only).
     *
     * @return a list of all device response DTOs
     * @throws DeviceFetchException if no devices are found
     */
    public List<DeviceResponse> getAllDevices() {
        List<Device> devices = deviceRepository.findAll();
        if (devices.isEmpty()) {
            throw new DeviceFetchException("No devices found");
        }
        return DeviceDtoUtil.convertDevicesToResponse(devices);
    }

    /**
     * Updates fields of a specific device by its ID.
     * Admins can additionally update the technical name and firmware version.
     *
     * @param deviceId      the ID of the device to update
     * @param req           the device update request DTO
     * @param technicalName (Admin only) the new technical name for the device
     * @param firmware      (Admin only) the new firmware version for the device
     * @return the updated device response DTO
     * @throws DeviceNotFoundException  if the device is not found
     * @throws IllegalArgumentException if the device does not belong to the authenticated user
     */
    public DeviceResponse updateDeviceById(
            Long deviceId, DeviceUpdateRequest req, String technicalName, String firmware) {

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        // If either technicalName or firmware is null, it's a user request; verify ownership
        if (technicalName == null || firmware == null) {
            User currentUser = userService.getCurrentUserWithoutDevices();
            if (!Objects.equals(device.getUser(), currentUser)) {
                throw new IllegalArgumentException("Device does not belong to the authenticated user");
            }
        }

        if (technicalName != null && !technicalName.isEmpty()) {
            device.setTechnicalName(technicalName);
        }
        if (firmware != null && !firmware.isEmpty()) {
            device.setFirmware(firmware);
        }
        if (req.getName() != null && !req.getName().isEmpty()) {
            device.setName(req.getName());
        }
        if (req.getDisplayOrder() != null && req.getDisplayOrder() >= 0) {
            device.setDisplayOrder(req.getDisplayOrder());
        }

        return DeviceDtoUtil.convertDeviceToResponse(deviceRepository.save(device));
    }

    /**
     * Overloaded method for updating a device by its ID for authenticated users.
     * Users can only update the user-defined name and display order of their own devices.
     *
     * @param deviceId the ID of the device to update
     * @param req      the device update request DTO
     * @return the updated device response DTO
     */
    public DeviceResponse updateUserDeviceById(Long deviceId, DeviceUpdateRequest req) {
        return updateDeviceById(deviceId, req, null, null);
    }

    /**
     * Delete a device by its ID (Admin only).
     *
     * @param deviceId the ID of the device to delete
     * @throws DeviceNotFoundException if the device is not found
     */
    public void deleteDeviceById(Long deviceId) {
        Device device = deviceRepository.findById(deviceId).orElseThrow(() -> new DeviceNotFoundException(deviceId));
        deviceRepository.delete(device);
    }

    /*--------------------------*/
    /*      Helper Methods      */
    /*--------------------------*/

    /**
     * Converts a DeviceProvisionRequest DTO to a Device entity.
     *
     * @param req the device provision request DTO
     * @return the device entity
     */
    private Device convertRequestToDevice(DeviceProvisionRequest req) {
        Device device = new Device();
        device.setTechnicalName(req.getTechnicalName());
        device.setFirmware(req.getFirmware());
        device.setMacAddress(req.getMacAddress());
        return device;
    }

    /**
     * Converts MqttCredentials entity to MqttCredentialsResponse DTO.
     *
     * @param mqttCredentials the MQTT credentials entity
     * @return the MQTT credentials response DTO
     */
    private MqttCredentialsResponse convertMqttCredentialsToResponse(MqttCredentials mqttCredentials) {
        return new MqttCredentialsResponse(mqttCredentials.getUsername(), mqttCredentials.getPassword());
    }

    /**
     * Ensures MQTT credentials exist for the user, creating them if not present.
     *
     * @param user the user to check/create credentials for
     */
    private void ensureMqttCredentialsForUser(User user) {
        if (!mqttCredentialsRepository.existsByUserId(user.getId())) {
            String encodedPassword = encoderService.hmacSha256Encoder().apply(privateKey, user.getId().toString());
            MqttCredentials mqttCredentials = new MqttCredentials();
            mqttCredentials.setUsername(user.getUsername());
            mqttCredentials.setPassword(encodedPassword);
            mqttCredentials.setUser(user);
            mqttCredentialsRepository.save(mqttCredentials);
        }
    }

    /**
     * Calculates the next display order for a user's devices.
     *
     * @param user the user whose devices are being ordered
     * @return the next display order
     */
    private int calculateDeviceDisplayOrder(User user) {
        return user.getDevices() != null && !user.getDevices().isEmpty()
                ? user.getDevices().stream()
                .mapToInt(Device::getDisplayOrder)
                .max()
                .orElse(0) + 1
                : 1;
    }

}