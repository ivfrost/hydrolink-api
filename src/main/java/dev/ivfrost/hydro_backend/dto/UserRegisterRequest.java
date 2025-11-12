package dev.ivfrost.hydro_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequest {

    @NotBlank
    @Email
    @Size(min = 5, max = 60)
    private String email;

    @NotBlank
    @Size(min = 5, max = 20)
    private String username;

    @NotBlank
    @Size(min = 6, max = 40)
    private String fullName;

    @NotBlank
    @Size(min = 8, max = 60)
    private String password;

    @Size(min = 2, max = 2)
    private String preferredLanguage;
}