package dev.ivfrost.hydro_backend.schedules.internal;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.iotdataplane.model.UpdateThingShadowRequest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Slf4j
@Component
@RequiredArgsConstructor
class ScheduleShadowPublisher {

  private static final int DAYS_AHEAD = 7;

  private final IotDataPlaneClient iotData;
  private final ScheduleRepository scheduleRepository;
  private final ObjectMapper objectMapper;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  void on(ScheduleChangedEvent e) {
    LocalDate today = LocalDate.now();
    List<Schedule> days =
        scheduleRepository.findByDeviceKeyAndDateBetween(
            e.deviceKey(), today, today.plusDays(DAYS_AHEAD));

    ObjectNode daysNode = objectMapper.createObjectNode();
    for (Schedule s : days) {
      ArrayNode windows = daysNode.putArray(s.getDate().toString());
      for (TimeWindow w : s.getWindows()) {
        windows.add(toJson(w));
      }
    }

    ObjectNode desired = objectMapper.createObjectNode();
    desired.put("version", e.version());
    desired.set("days", daysNode);

    ObjectNode doc = objectMapper.createObjectNode();
    doc.putObject("state").set("desired", desired);

    try {
      String payload = objectMapper.writeValueAsString(doc);
      iotData.updateThingShadow(UpdateThingShadowRequest.builder()
          .thingName(e.deviceKey())
          .shadowName("schedules")
          .payload(SdkBytes.fromUtf8String(payload))
          .build());
    } catch (Exception ex) {
      log.error("Shadow publish failed for {} v{}", e.deviceKey(), e.version(), ex);
    }
  }

  private ObjectNode toJson(TimeWindow w) {
    ObjectNode n = objectMapper.createObjectNode();
    n.put("pin", w.getPin());
    n.put("startType", w.getStartType().name());
    n.put("durationMinutes", w.getDurationMinutes());
    if (w.getFixedTime() != null) {
      n.put("fixedTime", w.getFixedTime().toString());
    }
    if (w.getLinkedPin() != null) {
      n.put("linkedPin", w.getLinkedPin());
    }
    if (w.getOffsetMinutes() != null) {
      n.put("offsetMinutes", w.getOffsetMinutes());
    }
    if (w.getLinkedReferencePoint() != null) {
      n.put("linkedReferencePoint", w.getLinkedReferencePoint().name());
    }
    return n;
  }
}