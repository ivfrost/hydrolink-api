package dev.ivfrost.hydro_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasswordResetRequest {

    @Email(message = "Invalid email format")
    private final String email;

    @Size(min = 11, max = 11, message = "Recovery code must be exactly 11 characters long")
    private final String recoveryCode;

    @Size(min = 8, max = 60, message = "Password must be between 8 and 60 characters long")
    private final String newPassword;
}
