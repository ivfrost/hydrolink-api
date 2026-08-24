package dev.ivfrost.hydro_backend.schedules.internal;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.DayOfWeek;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "day_of_week"}))
public class Schedule {
  @Id
  @GeneratedValue
  private Long id;

  @Column(name = "day_of_week", nullable = false)
  private DayOfWeek dayOfWeek;

  @Column(name = "device_key", nullable = false)
  private String deviceKey;

  @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<TimeWindow> windows;
}