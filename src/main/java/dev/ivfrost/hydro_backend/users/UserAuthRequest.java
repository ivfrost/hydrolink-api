package dev.ivfrost.hydro_backend.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserAuthRequest(

    @NotNull(message = "Email is required")
    @Email(message = "Invalid email")
    String email,

    @NotNull(message = "Password is required")
    @NotBlank
    String password) {

}
