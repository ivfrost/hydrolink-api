package dev.ivfrost.hydro_backend.exception;

public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(Long deviceId) {
        super("Device with ID " + deviceId + " not found.");
    }

    public DeviceNotFoundException(String hash) {
        super("Device with hash " + hash + " not found.");
    }
}
