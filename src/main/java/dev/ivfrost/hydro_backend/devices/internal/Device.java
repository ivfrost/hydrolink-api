package dev.ivfrost.hydro_backend.devices.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "devices", indexes = @Index(columnList = "user_id"))
@Entity
public class Device implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(nullable = false, unique = true)
  private String key;

  @Size(max = 17, min = 17)
  @Column(nullable = false, name = "mac_address", unique = true)
  private String macAddress;

  @Size(min = 1, max = 40)
  @Column(name = "friendly_name")
  private String friendlyName;

  @Size(max = 255)
  private String location;

  @Size(max = 20)
  @Column(nullable = false)
  private String firmware;

  @Size(max = 40)
  @Column(name = "technical_name", nullable = false)
  private String technicalName;

  @Size(max = 255)
  @Column(name = "secret", unique = true)
  private String secret;

  @Size(max = 255)
  @Column(name = "description")
  private String description;

  @Pattern(regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$")
  @Size(max = 15)
  @Column
  private String ip;

  @Column(name = "image_url")
  private String imageUrl;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP(6) WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
  private Instant updatedAt;

  @Column(name = "linked_at", columnDefinition = "TIMESTAMP(6) WITH TIME ZONE")
  private Instant linkedAt;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "display_order")
  private Long displayOrder;
}