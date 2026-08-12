package dev.ivfrost.hydro_backend.storage.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "ota_updates")
@Entity
public class OtaUpdate implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(nullable = false)
  private String version;

  @Column(name = "technical_name", nullable = false)
  private String technicalName;

  @Column(name = "object_key", nullable = false)
  private String objectKey;

  @Column(nullable = false)
  private String sha256;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(name = "force_install", nullable = false)
  private boolean forceInstall;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
  private Instant createdAt;
}