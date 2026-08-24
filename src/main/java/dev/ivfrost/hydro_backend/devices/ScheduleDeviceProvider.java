package dev.ivfrost.hydro_backend.devices;

public interface ScheduleDeviceProvider {

  public DeviceResponse getDeviceByKey(String deviceKey);
  void requireDeviceByKey(String deviceKey);

}
