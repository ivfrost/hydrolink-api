package dev.ivfrost.hydro_backend.users;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ImportRuntimeHints;

public record UserRegisterRequest(
    @Email
    @NotNull
    @Size(min = 5, max = 60)
    @Schema(requiredMode = RequiredMode.REQUIRED)
    String email,

    @NotNull
    @Size(min = 5, max = 20)
    @Schema(requiredMode = RequiredMode.REQUIRED)
    String username,

    @NotNull
    @Size(min = 6, max = 40)
    @Schema(requiredMode = RequiredMode.REQUIRED)
    String fullName,

    @NotNull
    @Size(min = 8, max = 42)
    @Schema(requiredMode = RequiredMode.REQUIRED)
    String password
) {

}

@ImportRuntimeHints(value = UserRegisterRequestRuntimeHints.class)
class UserRegisterRequestRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(org.springframework.aot.hint.RuntimeHints hints,
      ClassLoader classLoader) {
    hints.reflection().registerType(
        UserRegisterRequest.class,
        org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS,
        org.springframework.aot.hint.MemberCategory.ACCESS_DECLARED_FIELDS);
  }
}