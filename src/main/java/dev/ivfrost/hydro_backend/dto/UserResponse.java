package dev.ivfrost.hydro_backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import dev.ivfrost.hydro_backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserResponse {

    private Long id;

    private String username;

    private String fullName;

    private String email;

    private String profilePictureUrl;

    private String phoneNumber;

    private String address;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant lastLogin;

    private User.Role role;

    private String preferredLanguage;

    private JsonNode settings;

    private List<DeviceResponse> devices;
}
