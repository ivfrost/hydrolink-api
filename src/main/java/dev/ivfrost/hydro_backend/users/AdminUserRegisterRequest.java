package dev.ivfrost.hydro_backend.users;

import dev.ivfrost.hydro_backend.users.internal.UserRole.Role;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Data;

@Data
public class AdminUserRegisterRequest {

  @Valid
  private UserRegisterRequest userDetails;

  private List<Role> roles;
}