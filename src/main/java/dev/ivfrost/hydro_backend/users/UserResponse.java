package dev.ivfrost.hydro_backend.users;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse {

  @NotNull
  Long id;

  @NotNull
  String username;

  @NotNull
  String fullName;

  @NotNull
  String email;

  String profilePictureUrl;

  // must be present (not null) but may be empty string
  @NotNull
  String phoneNumber;

  // must be present (not null) but may be empty string
  @NotNull
  String address;

  @NotNull
  Instant createdAt;

  @NotNull
  Instant updatedAt;

  @NotNull
  List<String> roles;

  @NotNull
  String settings;
}
