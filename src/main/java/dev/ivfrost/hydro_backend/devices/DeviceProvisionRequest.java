package dev.ivfrost.hydro_backend.devices;

import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.ImportRuntimeHints;

public record DeviceProvisionRequest(
    @Size(max = 20)
    String firmware,
    @Size(max = 40)
    String technicalName,
    @Size(max = 12, min = 12)
    String key,
    @Size(max = 17, min = 17)
    String macAddress) {

}

@ImportRuntimeHints(value = DeviceProvisionRequestRuntimeHints.class)
class DeviceProvisionRequestRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    hints.reflection().registerType(
        DeviceProvisionRequest.class,
        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        MemberCategory.INVOKE_DECLARED_METHODS,
        MemberCategory.ACCESS_DECLARED_FIELDS);
  }
}