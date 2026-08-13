package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.devices.PinMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pins", uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "pin_number"}))
public class Pin {
  @Id
  @GeneratedValue
  private Long id;
  @Column(name = "pin_number", nullable = false)
  private Integer pinNumber;
  @Enumerated(EnumType.STRING)
  @Column(name = "mode", nullable = false)
  private PinMode mode;
  private String label;
  @ManyToOne
  @JoinColumn(name = "device_id", nullable = false)
  private Device device;
}