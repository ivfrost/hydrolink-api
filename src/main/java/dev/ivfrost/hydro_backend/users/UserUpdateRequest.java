package dev.ivfrost.hydro_backend.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
    @Size(min = 5, max = 20)
    String username,

    @Size(min = 8, max = 42)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String password,

    @Size(min = 8, max = 42)
    String currentPassword,

    @Size(min = 6, max = 40)
    String fullName,

    @Email(message = "Invalid email format")
    @Size(min = 8, max = 50)
    String email,

    @Pattern(regexp = "^$|^\\+?[0-9\\-\\s]{7,20}$", message = "Invalid phone number format")
    @Size(max = 20)
    String phoneNumber,

    @Size(max = 100)
    String address,

    String settings
) {

}
