package dev.ivfrost.hydro_backend.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ImportRuntimeHints;

public record UserLoginRequest(
    @Email
    @Size(min = 5, max = 60)
    String email,
    @Size(min = 8, max = 42)
    @JsonProperty(access = Access.WRITE_ONLY)
    String password) {

}

@ImportRuntimeHints(value = UserLoginRequestRuntimeHints.class)
class UserLoginRequestRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    hints.reflection().registerType(
        UserLoginRequest.class,
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_DECLARED_METHODS,
        MemberCategory.ACCESS_DECLARED_FIELDS);
  }
}