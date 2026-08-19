package dev.ivfrost.hydro_backend.users;

import dev.ivfrost.hydro_backend.users.internal.UserRole.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Data;

@Data
public class AdminUserRegisterRequest {

  @Schema(
      description = "Details of the user to be registered",
      requiredMode = Schema.RequiredMode.REQUIRED,
      implementation = UserRegisterRequest.class
  )
  @Valid
  private UserRegisterRequest userDetails;

  @Schema(
      example = "[\"ADMIN\", \"USER\"]",
      description = "List of roles to be assigned to the new user",
      requiredMode = Schema.RequiredMode.REQUIRED,
      implementation = Role.class
  )
  private List<Role> roles;
}