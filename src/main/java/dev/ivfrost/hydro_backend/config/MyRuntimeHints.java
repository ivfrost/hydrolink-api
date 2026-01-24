package dev.ivfrost.hydro_backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ivfrost.hydro_backend.devices.DeviceLinkRequest;
import dev.ivfrost.hydro_backend.devices.DeviceProvisionRequest;
import dev.ivfrost.hydro_backend.users.UserLoginRequest;
import dev.ivfrost.hydro_backend.users.UserRegisterRequest;
import dev.ivfrost.hydro_backend.users.internal.User;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

// Specify to Spring AOT that these classes will be need to be accessed via reflection
public class MyRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(org.springframework.aot.hint.RuntimeHints hints,
      ClassLoader classLoader) {
    hints.reflection().registerType(JsonNode.class);
    hints.reflection().registerType(ObjectMapper.class);
    hints.reflection()
        .registerType(
            TypeReference.of("org.springframework.core.annotation.TypeMappedAnnotation[]"),
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

    // Register DTOs for reflection
    hints.reflection()
        .registerType(DeviceLinkRequest.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS);
    hints.reflection()
        .registerType(DeviceProvisionRequest.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS);
    hints.reflection()
        .registerType(UserRegisterRequest.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS);
    hints.reflection()
        .registerType(UserLoginRequest.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS);

    // Validation of individual entity fields with reflection
    hints.reflection()
        .registerType(User.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_DECLARED_METHODS, MemberCategory.DECLARED_FIELDS);
  }

}
