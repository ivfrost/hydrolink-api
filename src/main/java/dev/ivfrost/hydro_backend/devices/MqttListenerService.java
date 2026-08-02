package dev.ivfrost.hydro_backend.devices;

import dev.ivfrost.hydro_backend.config.ApiProperties;
import dev.ivfrost.hydro_backend.config.MqttProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
class MqttListenerService {

  private final MqttProperties mqttProperties;
  private final ApiProperties apiProperties;

  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void init() {
    try {
      Thread.sleep(2000);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      return;
    }

    int attempts = 0;
    int maxAttempts = 5;
    while (attempts < maxAttempts) {
      try {
        attempts++;
        MqttClient client = new MqttClient(mqttProperties.brokerUrl(), apiProperties.mqttClientId(),
            new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(apiProperties.mqttUsername());
        options.setPassword(apiProperties.mqttPassword().toCharArray());
        options.setAutomaticReconnect(true);

        client.setCallback(new MqttCallback() {
          @Override
          public void connectionLost(Throwable cause) {
            log.warn("MQTT connection lost: {}", cause.getMessage());
          }

          @Override
          public void messageArrived(String topic, MqttMessage message) {
            try {
              String[] tokens = topic.split("/");
              String key = tokens[1];
              log.debug("Received MQTT message from device {}: {}", key, new String(message.getPayload()));
            } catch (Exception e) {
              log.error("Error processing MQTT message: {}", e.getMessage());
            }
          }

          @Override
          public void deliveryComplete(IMqttDeliveryToken token) {
          }
        });

        client.connect(options);
        client.subscribe(mqttProperties.topicWildcard());
        log.info("MQTT client connected successfully to {}", mqttProperties.brokerUrl());
        return;
      } catch (MqttException e) {
        log.warn("MQTT connection attempt {}/{} failed", attempts, maxAttempts, e);
        if (attempts < maxAttempts) {
          try {
            Thread.sleep(5000);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
          }
        }
      }
    }
    log.error("Failed to connect to MQTT broker after {} attempts", maxAttempts);
  }
}