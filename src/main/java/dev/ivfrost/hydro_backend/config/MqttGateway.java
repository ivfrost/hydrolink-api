package dev.ivfrost.hydro_backend.config;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

@MessagingGateway
public interface MqttGateway {
  void sendToMqtt(@Payload String payload, @Header("mqtt_topic") String topic);

  /**
   * Publishes a retained MQTT message. Retained messages are delivered to any
   * subscriber that connects later — used for OTA announcements so a phone that
   * subscribes after the backend publishes still receives the notification.
   */
  void sendRetainedToMqtt(@Payload String payload, @Header("mqtt_topic") String topic,
      @Header(MqttHeaders.RETAINED) boolean retained);
}
