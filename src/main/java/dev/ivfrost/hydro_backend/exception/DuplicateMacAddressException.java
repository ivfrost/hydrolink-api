package dev.ivfrost.hydro_backend.exception;

public class DuplicateMacAddressException extends RuntimeException {

  public DuplicateMacAddressException(String macAddress) {
    super("Device with MAC address '" + macAddress + "' already exists");
  }

  public DuplicateMacAddressException(String macAddress, Throwable cause) {
    super("Device with MAC address '" + macAddress + "' already exists", cause);
  }
}

