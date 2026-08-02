package dev.ivfrost.hydro_backend.tokens.internal;

import dev.ivfrost.hydro_backend.tokens.DeviceKeyEncriptionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JPA converter that automatically encrypts token value when storing to database and decrypts when
 * retrieving from database.
 */
@Slf4j
@Converter
@Component
class TokenValueConverter implements AttributeConverter<String, String> {

  private final DeviceKeyEncriptionUtil encryptionUtil;
  private final String recoverySecret;

  public TokenValueConverter(
      DeviceKeyEncriptionUtil encryptionUtil,
      @Value("${user.recovery-secret}") String recoverySecret) {
    this.encryptionUtil = encryptionUtil;
    this.recoverySecret = recoverySecret;
  }

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null) {
      return null;
    }
    checkInitialized();
    return encryptionUtil.encrypt(attribute, recoverySecret);
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    checkInitialized();
    return encryptionUtil.decrypt(dbData, recoverySecret);
  }

  private void checkInitialized() {
    if (encryptionUtil == null) {
      log.error("EncryptionUtil not initialized - cannot encrypt");
      throw new IllegalStateException("EncryptionUtil not initialized - cannot encrypt");
    }
    if (recoverySecret == null) {
      log.error("Recovery secret not initialized - cannot encrypt");
      throw new IllegalStateException("Recovery secret not initialized - cannot encrypt");
    }
  }
}