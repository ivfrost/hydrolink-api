package dev.ivfrost.hydro_backend.config;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

@MessagingGateway
public interface MqttGateway {
  void sendToMqtt(@Payload String payload, @Header("mqtt_topic") String topic);
}