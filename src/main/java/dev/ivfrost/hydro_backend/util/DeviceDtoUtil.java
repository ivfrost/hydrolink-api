package dev.ivfrost.hydro_backend.util;

import dev.ivfrost.hydro_backend.dto.DeviceResponse;
import dev.ivfrost.hydro_backend.entity.Device;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DeviceDtoUtil {

    /**
     * Converts a Device entity to a DeviceResponse DTO.
     *
     * @param device the device entity
     * @return the device response DTO
     */
    public static DeviceResponse convertDeviceToResponse(Device device) {
        if (device == null) return null;
        return new DeviceResponse(
                device.getId(),
                device.getName(),
                device.getLocation(),
                device.getFirmware(),
                device.getTechnicalName(),
                device.getIp(),
                device.getCreatedAt(),
                device.getUpdatedAt(),
                device.getLinkedAt(),
                device.getLastSeen(),
                device.getUser(),
                device.getDisplayOrder() != null ? device.getDisplayOrder() : 0
        );
    }

    /**
     * Converts a list of Device entities to a list of DeviceResponse DTOs.
     *
     * @param devices the list of device entities
     * @return the list of device response DTOs
     */
    public static List<DeviceResponse> convertDevicesToResponse(List<Device> devices) {
        return devices.stream()
                .map(DeviceDtoUtil::convertDeviceToResponse)
                .collect(Collectors.toList());
    }
}

