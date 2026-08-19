package dev.ivfrost.hydro_backend.users;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserAuthRequest(

    @Schema(example = "admin@hydrolink.io", requiredMode = RequiredMode.REQUIRED)
    @NotBlank
    @Email
    String email,

    @NotBlank
    @Schema(example = "secret123", requiredMode = RequiredMode.REQUIRED)
    String password) {

}
