package dev.ivfrost.hydro_backend.devices;

import org.springframework.http.HttpStatus;

/**
 * Signals an unknown SKU code or an attempt to change the SKU of an existing device row.
 */
public class DeviceSkuException extends RuntimeException {

  private final HttpStatus status;

  private DeviceSkuException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public static DeviceSkuException unknown(String sku) {
    return new DeviceSkuException(HttpStatus.BAD_REQUEST, "Unknown SKU: " + sku);
  }

  public static DeviceSkuException mismatch(String deviceKey, String currentSku, String requestedSku) {
    return new DeviceSkuException(HttpStatus.CONFLICT,
        "Device " + deviceKey + " is registered with SKU " + currentSku
            + " and cannot change to " + requestedSku);
  }

  public HttpStatus getStatus() {
    return status;
  }
}
