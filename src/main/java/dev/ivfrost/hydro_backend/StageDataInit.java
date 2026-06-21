package dev.ivfrost.hydro_backend;

import dev.ivfrost.hydro_backend.devices.internal.Device;
import dev.ivfrost.hydro_backend.devices.internal.DeviceRepository;
import dev.ivfrost.hydro_backend.tokens.EncryptionUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("stage")
@RequiredArgsConstructor
@Component
public class StageDataInit implements ApplicationRunner {

  private final DeviceRepository deviceRepository;
  private final EncryptionUtil encryptionUtil;
  @Value("${seed.device1.key}")
  private String device1Key;
  @Value("${seed.device1.secret}")
  private String device1Secret;
  @Value("${seed.device2.key}")
  private String device2Key;
  @Value("${seed.device2.secret}")
  private String device2Secret;

  @Override
  public void run(@NonNull ApplicationArguments args) {
    if (deviceRepository.findByKey(device1Key).isEmpty()) {
      deviceRepository.save(Device.builder()
          .key(device1Key)
          .macAddress("00:11:22:33:44:55")
          .firmware("1.0.0")
          .secret(encryptionUtil.encrypt(device1Secret))
          .technicalName("hydrolink-esp32")
          .friendlyName("Greenhouse Irrigation Controller")
          .location("Greenhouse")
          .build());
    }
    if (deviceRepository.findByKey(device2Key).isEmpty()) {
      deviceRepository.save(Device.builder()
          .key(device2Key)
          .macAddress("66:77:88:99:AA:BB")
          .firmware("1.0.0")
          .secret(encryptionUtil.encrypt(device2Secret))
          .technicalName("hydrolink-esp32")
          .friendlyName("Garden Irrigation Controller")
          .location("Garden")
          .build());
    }
  }
}
