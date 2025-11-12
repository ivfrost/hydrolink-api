package dev.ivfrost.hydro_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class UserRegisterResponse {

    String[] recoveryCodes;
}
