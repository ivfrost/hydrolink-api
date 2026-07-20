package dev.ivfrost.hydro_backend.tokens.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Data;

@Data
@Entity
@Table(name = "tokens", indexes = @Index(columnList = "user_id"))
public class Token {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;
  @Convert(converter = TokenValueConverter.class)
  @Size(max = 255)
  @Column(nullable = false, unique = true)
  private String value;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TokenType type;
  @Column(name = "expiry_date", nullable = true, columnDefinition = "TIMESTAMP WITH TIME ZONE")
  private Instant expiryDate;
  @Column(name = "user_id", nullable = false)
  private long userId;

  public enum TokenType {
    AUTH_RECOVERY_CODE,
    AUTH_ACCESS_TOKEN,
    AUTH_REFRESH_TOKEN,
    MQTT_TOKEN,
    DEVICE_MQTT_TOKEN
  }

}
