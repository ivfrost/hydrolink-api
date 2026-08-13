package dev.ivfrost.hydro_backend.devices.internal;

import dev.ivfrost.hydro_backend.devices.LinkedReferencePoint;
import dev.ivfrost.hydro_backend.devices.TimeWindowStartType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
@Entity
public class TimeWindow {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "pin", nullable = false)
  private Integer pin;

  @Enumerated(EnumType.STRING)
  @Column(name = "start_type", nullable = false)
  private TimeWindowStartType startType;

  @Column(name = "fixed_time")
  private LocalTime fixedTime;

  @Column(name = "linked_pin")
  private Integer linkedPin;

  @Column(name = "offset_minutes")
  private Integer offsetMinutes;

  @Column(name = "duration_minutes", nullable = false)
  private Integer durationMinutes;

  @Enumerated(EnumType.STRING)
  @Column(name = "linked_reference_point")
  private LinkedReferencePoint linkedReferencePoint;

  @ManyToOne
  @JoinColumn(name = "schedule_id", nullable = false)
  private Schedule schedule;

  protected TimeWindow() {}
}

