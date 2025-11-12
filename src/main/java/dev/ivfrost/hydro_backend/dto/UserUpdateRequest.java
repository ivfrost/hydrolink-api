package dev.ivfrost.hydro_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserUpdateRequest {
    @Size(min = 5, max = 20)
    private String username;

    @Size(min = 4, max = 40)
    private String fullName;

    @Email(message = "Invalid email format")
    @Size(min = 8, max = 50)
    private String email;

    @Size(max = 255)
    private String profilePictureUrl;

    @Pattern(regexp = "^\\+?[0-9\\-\\s]{7,20}$", message = "Invalid phone number format")
    @Size(max = 20)
    private String phoneNumber;

    @Size(max = 100)
    private String address;

    @Size(min = 2, max = 2)
    private String preferredLanguage = "es";

    private JsonNode settings = new ObjectMapper().createObjectNode();
}
