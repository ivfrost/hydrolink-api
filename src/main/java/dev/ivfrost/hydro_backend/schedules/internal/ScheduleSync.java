package dev.ivfrost.hydro_backend.schedules.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "schedule_sync")
@Entity
public class ScheduleSync {

  @Id
  @Column(name = "device_key", nullable = false)
  private String deviceKey;

  // Not a hibernate optimistic lock
  @Column(name = "version", nullable = false)
  private Long version;
}
