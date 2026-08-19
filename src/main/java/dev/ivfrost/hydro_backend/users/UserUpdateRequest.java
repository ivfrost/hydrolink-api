package dev.ivfrost.hydro_backend.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UserUpdateRequest(
    @Schema(description = "The username of the user", example = "test_user", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(min = 5, max = 20)
    String username,

    @Schema(description = "The new password of the user", example = "new_secure_password", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(min = 8, max = 42)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String password,

    @Schema(description = "The current password of the user", example = "current_password", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 8, max = 42)
    String currentPassword,

    @Schema(description = "The full name of the user", example = "Test User Updated", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(min = 6, max = 40)
    String fullName,

    @Schema(description = "The email of the user", example = "updated_user@hydrolink.io", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Email(message = "Invalid email format")
    @Size(min = 8, max = 50)
    String email,

    @Pattern(regexp = "^$|^\\+?[0-9\\-\\s]{7,20}$", message = "Invalid phone number format")
    @Schema(description = "The phone number of the user", example = "+34923445222", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 20)
    String phoneNumber,

    @Schema(description = "The address of the user", example = "123 Main St, City, Country", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 100)
    String address,

    @Schema(description = "The image URL of the user", example = "https://hydrolink.io/v1/storage/users/102/upload", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @URL
    String imageUrl,

    @Schema(description = "The settings of the user", example = "{\"notifications\":true}", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String settings
) {

}
