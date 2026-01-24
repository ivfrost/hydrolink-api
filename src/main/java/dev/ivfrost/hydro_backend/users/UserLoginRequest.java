package dev.ivfrost.hydro_backend.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserLoginRequest(
    @Email
    @Size(min = 5, max = 60)
    String email,
    @Size(min = 8, max = 42)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String password) {

}
