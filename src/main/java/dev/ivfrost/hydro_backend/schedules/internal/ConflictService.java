package dev.ivfrost.hydro_backend.schedules.internal;

import dev.ivfrost.hydro_backend.devices.LinkedReferencePoint;
import dev.ivfrost.hydro_backend.schedules.TimeWindowStartType;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ConflictService {

  public List<Long> detectConflicts(List<TimeWindow> windows) {
    Map<Long, ResolvedWindow> resolved = resolveAll(windows);
    Set<Long> conflicting = new HashSet<>();

    Map<Integer, List<ResolvedWindow>> byPin = resolved.values().stream()
        .collect(Collectors.groupingBy(ResolvedWindow::pin));

    for (List<ResolvedWindow> group : byPin.values()) {
      for (int i = 0; i < group.size(); i++) {
        for (int j = i + 1; j < group.size(); j++) {
          ResolvedWindow a = group.get(i);
          ResolvedWindow b = group.get(j);
          if (overlaps(a, b)) {
            conflicting.add(a.id());
            conflicting.add(b.id());
          }
        }
      }
    }

    return new ArrayList<>(conflicting);
  }

  private boolean overlaps(ResolvedWindow a, ResolvedWindow b) {
    return a.start().isBefore(b.end()) && b.start().isBefore(a.end());
  }

  private Map<Long, ResolvedWindow> resolveAll(List<TimeWindow> windows) {
    Map<Integer, TimeWindow> byPin = windows.stream()
        .collect(Collectors.toMap(TimeWindow::getPin, w -> w));

    Map<Long, ResolvedWindow> result = new HashMap<>();
    for (TimeWindow w : windows) {
      LocalTime start = resolveStart(w, byPin, new HashSet<>());
      if (start == null) {
        throw new IllegalArgumentException("Unable to resolve start time for window id " + w.getId() +
            " (pin=" + w.getPin() + ", startType=" + w.getStartType() + ")");
      }
      LocalTime end = start.plusMinutes(w.getDurationMinutes());
      result.put(w.getId(), new ResolvedWindow(w.getId(), w.getPin(), start, end));
    }
    return result;
  }

  private LocalTime resolveStart(TimeWindow w, Map<Integer, TimeWindow> byPin, Set<Integer> visiting) {
    if (w.getStartType() == TimeWindowStartType.FIXED) {
      if (w.getFixedTime() == null) {
        throw new IllegalArgumentException("Fixed time is required for window with pin " + w.getPin());
      }
      return w.getFixedTime();
    }

    if (!visiting.add(w.getPin())) {
      throw new IllegalStateException("Circular reference detected in linked schedule for pin " + w.getPin());
    }

    TimeWindow reference = byPin.get(w.getLinkedPin());
    if (reference == null) {
      throw new IllegalStateException("Linked pin " + w.getLinkedPin() + " not found in schedule");
    }
    LocalTime refStart = resolveStart(reference, byPin, visiting);
    if (refStart == null) {
      throw new IllegalStateException("Referenced pin " + w.getLinkedPin() + " could not be resolved");
    }

    LocalTime anchorPoint = (w.getLinkedReferencePoint() == LinkedReferencePoint.START)
        ? refStart
        : refStart.plusMinutes(reference.getDurationMinutes());

    return anchorPoint.plusMinutes(w.getOffsetMinutes());
  }

  private record ResolvedWindow(Long id, int pin, LocalTime start, LocalTime end) {}
}