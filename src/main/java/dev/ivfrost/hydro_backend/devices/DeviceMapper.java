package dev.ivfrost.hydro_backend.devices;

import dev.ivfrost.hydro_backend.devices.internal.Device;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DeviceMapper {

  DeviceResponse deviceToDeviceResponse(Device device);

  @Mapping(target = "secret", source = "rawSecret")
  DeviceProvisionResponse deviceToDeviceProvisionResponse(Device device, String rawSecret);

  Device deviceProvisionRequestToDevice(DeviceProvisionRequest deviceProvisionRequest);

  // Ignore manually validated properties
  @Mapping(target = "technicalName", ignore = true)
  @Mapping(target = "firmware", ignore = true)
  @Mapping(target = "userId", ignore = true)
  void updateDeviceFromRequest(DeviceUpdateRequest req, @MappingTarget Device device);

  @Mapping(target = "userId", ignore = true)
  DeviceUpdateRequest adminToNonAdminDeviceUpdateRequest(AdminDeviceUpdateRequest adminDeviceUpdateRequest);
}
