package dev.ivfrost.hydro_backend.tokens.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "tokens")
public class Token {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;
  @Convert(converter = TokenValueConverter.class)
  @Size(max = 255)
  @Column(nullable = false)
  private String value;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TokenType type;
  @Column(name = "expiry_date", nullable = true)
  private LocalDateTime expiryDate;
  @Column(name = "user_id", nullable = false)
  private long userId;

  public enum TokenType {
    RECOVERY_CODE,
    AUTH_ACCESS_TOKEN,
    AUTH_REFRESH_TOKEN

  }

}
