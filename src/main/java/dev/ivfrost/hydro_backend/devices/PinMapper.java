package dev.ivfrost.hydro_backend.devices;

import dev.ivfrost.hydro_backend.devices.internal.Pin;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PinMapper {

  PinResponse pinToPinResponse(Pin pin);

  List<Pin> pinResponseToPin(List<PinResponse> pinResponse);

  Pin pinRequestToPin(PinRequest pinRequest);
  Set<Pin> pinRequestToPin(Set<PinRequest> pinRequests);
}
