package dev.ivfrost.hydro_backend.devices;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public record DeviceLinkRequest(
    @Schema(example = "bc3e9dbdf08d73e760f00249da44dd68", description = "The secret used to link the device record to the user account. Issued when the device record is registered in the application, independent of the AWS Thing provisioning.")
    @NotBlank
    @Size(max = 32)
    String secret) {

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