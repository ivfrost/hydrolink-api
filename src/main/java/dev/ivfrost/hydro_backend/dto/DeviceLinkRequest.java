package dev.ivfrost.hydro_backend.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceLinkRequest {

    @Column(length = 44, nullable = false)
    private String hash;
}