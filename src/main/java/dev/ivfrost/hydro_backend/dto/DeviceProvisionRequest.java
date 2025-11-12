package dev.ivfrost.hydro_backend.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceProvisionRequest {

    @Size(max = 20, min = 4)
    private String firmware;

    @Size(max = 40, min = 4)
    private String technicalName;

    @Size(max = 100, min = 4)
    private String macAddress;
}
