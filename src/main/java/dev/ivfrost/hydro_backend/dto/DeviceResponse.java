package dev.ivfrost.hydro_backend.dto;

import dev.ivfrost.hydro_backend.entity.User;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DeviceResponse {
    private Long id;
    private String name;
    private String location;
    private String firmware;
    private String technicalName;
    private String ip;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant linkedAt;
    private Instant lastSeen;
    private User user;
    private Integer displayOrder;
}
