package dev.ivfrost.hydro_backend.tokens;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * JPA converter that automatically encrypts recovery secrets when storing to database and decrypts
 * when retrieving from database.
 */
@Slf4j
@RequiredArgsConstructor
@Converter
@Component
public class DeviceSecretConverter implements AttributeConverter<String, String> {

  private final EncryptionUtil encryptionUtil;
  @Value("${device.secret}")
  private String deviceSecret;

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null) {
      return null;
    }
    checkInitialized();
    String encrypted = encryptionUtil.encrypt(attribute, deviceSecret);
    log.info("DEBUG CONVERTER - Encrypting '{}' -> '{}'", attribute, encrypted);
    return encrypted;
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    checkInitialized();
    String decrypted = encryptionUtil.decrypt(dbData, deviceSecret);
    log.info("DEBUG CONVERTER - Decrypting '{}' -> '{}'", dbData, decrypted);
    return decrypted;
  }

  private void checkInitialized() {
    if (encryptionUtil == null) {
      log.error("EncryptionUtil not initialized - cannot encrypt");
      throw new IllegalStateException("EncryptionUtil not initialized - cannot encrypt");
    }
    if (deviceSecret == null) {
      log.error("Recovery secret not initialized - cannot encrypt");
      throw new IllegalStateException("Device secret not initialized - cannot encrypt");
    }
  }
}

