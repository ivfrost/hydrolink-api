package dev.ivfrost.hydro_backend.exception;

public class DeviceLinkException extends RuntimeException {

    public DeviceLinkException(String hash) {
        super("Device with hash " + hash + " is already linked to a user.");
    }
}
