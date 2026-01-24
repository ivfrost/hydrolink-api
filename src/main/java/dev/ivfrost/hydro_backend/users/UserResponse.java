package dev.ivfrost.hydro_backend.users;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserResponse implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;
  Long id;
  String username;
  String fullName;
  String email;
  String profilePictureUrl;
  String phoneNumber;
  String address;
  Instant createdAt;
  Instant updatedAt;
  List<String> roles;
  String preferredLanguage;
  String settings;
}
