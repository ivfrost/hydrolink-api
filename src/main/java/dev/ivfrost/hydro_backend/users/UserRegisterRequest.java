package dev.ivfrost.hydro_backend.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
    @Email
    @NotNull
    @Size(min = 5, max = 60)
    String email,

    @NotNull
    @Size(min = 5, max = 20)
    String username,

    @NotNull
    @Size(min = 6, max = 40)
    String fullName,

    @NotNull
    @Size(min = 8, max = 42)
    String password,

    @NotNull
    @Size(min = 2, max = 2)
    String preferredLanguage
) {

}
