package dev.ivfrost.hydro_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MqttCredentialsResponse {
    public String username;
    public String password;
}
