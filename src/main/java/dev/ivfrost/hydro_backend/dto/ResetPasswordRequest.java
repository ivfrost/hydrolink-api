package dev.ivfrost.hydro_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank
    @Email
    String email;

    @NotBlank
    @Size(min = 16, max = 16)
    String recoveryCode;

    @NotBlank
    @Size(min = 8, max = 60)
    String newPassword;
}
