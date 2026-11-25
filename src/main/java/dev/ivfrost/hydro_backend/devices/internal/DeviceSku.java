package dev.ivfrost.hydro_backend.devices.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * Hardware catalog row. The code is the immutable contract between firmware and API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "device_skus")
@Entity
public class DeviceSku {

  @Id
  @Column(length = 40)
  private String code;

  @Column(name = "display_name", nullable = false, length = 80)
  private String displayName;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> capabilities;

  @Builder.Default
  @Column(name = "ota_channel", nullable = false, length = 32)
  private String otaChannel = "stable";

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false,
      columnDefinition = "TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false,
      columnDefinition = "TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
  private Instant updatedAt;
}
