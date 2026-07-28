package dev.ivfrost.hydro_backend.devices;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public record DeviceLinkRequest(@NotBlank @Size(max = 32) String secret) {

}

class DeviceLinkRequestRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints,
      @Nullable ClassLoader classLoader) {
    hints.reflection().registerType(DeviceLinkRequest.class,
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_DECLARED_METHODS,
        MemberCategory.ACCESS_DECLARED_FIELDS);
  }
}