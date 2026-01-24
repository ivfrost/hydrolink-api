package dev.ivfrost.hydro_backend.users;

import jakarta.validation.constraints.NotBlank;

public record UserRefreshRequest(
    @NotBlank(message = "Refresh token is required")
    String refreshToken) {

}

