package dev.ivfrost.hydro_backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableIntegration
public class MqttConfig {

  private final MqttProperties mqttProperties;
  private final ApiProperties apiProperties;
  private final ObjectMapper objectMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Bean
  public MqttPahoClientFactory mqttClientFactory() {
    DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
    MqttConnectOptions options = new MqttConnectOptions();
    options.setServerURIs(new String[]{mqttProperties.brokerUrl()});
    options.setCleanSession(true);
    options.setAutomaticReconnect(true);
    options.setKeepAliveInterval(60);
    options.setUserName(apiProperties.mqttUsername());
    options.setPassword(apiProperties.mqttPassword().toCharArray());
    factory.setConnectionOptions(options);
    return factory;
  }

  @Bean
  public MessageChannel mqttOutboundChannel() {
    return new DirectChannel();
  }

  @Bean
  @ServiceActivator(inputChannel = "mqttOutboundChannel")
  public MessageHandler mqttOutboundHandler() {
    String clientId = "hydro-backend-publisher-" + getHostname() + "-" + UUID.randomUUID();
    MqttPahoMessageHandler handler = new MqttPahoMessageHandler(clientId, mqttClientFactory());
    handler.setAsync(true);
    handler.setDefaultQos(1);
    handler.setDefaultTopic("hydro/default");
    return handler;
  }

  @Bean
  public GatewayProxyFactoryBean mqttGateway() {
    GatewayProxyFactoryBean factory = new GatewayProxyFactoryBean(MqttGateway.class);
    factory.setDefaultRequestChannel(mqttOutboundChannel());
    return factory;
  }

  @Bean
  public MessageChannel mqttStatusInputChannel() {
    return new DirectChannel();
  }

  @Bean
  public MessageProducer mqttStatusInboundAdapter() {
    String clientId = "hydro-backend-status-subscriber-" + getHostname() + "-" + UUID.randomUUID();
    MqttPahoMessageDrivenChannelAdapter adapter =
        new MqttPahoMessageDrivenChannelAdapter(
            clientId,
            mqttClientFactory(),
            mqttProperties.topicWildcard());
    adapter.setCompletionTimeout(5000);
    adapter.setConverter(new DefaultPahoMessageConverter());
    adapter.setQos(1);
    adapter.setOutputChannel(mqttStatusInputChannel());
    return adapter;
  }

  @Bean
  public MessageChannel mqttErrorChannel() {
    return new DirectChannel();
  }

  @ServiceActivator(inputChannel = "mqttErrorChannel")
  public void handleMqttError(Message<?> errorMessage) {
    log.error("MQTT error: {}", errorMessage.getPayload(), errorMessage.getPayload());
  }

  @ServiceActivator(inputChannel = "mqttStatusInputChannel")
  public void handleStatusMessage(Message<?> message) {
    String payload = message.getPayload().toString();
    String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
    log.info("Received status from topic [{}]: {}", topic, payload);

    String deviceKey = extractDeviceKey(topic);
    if (deviceKey == null) {
      log.warn("Cannot extract deviceKey from topic: {}", topic);
      return;
    }

    try {
      if (StringUtils.isBlank(payload)) {
        log.warn("Received empty payload for deviceKey: {}", deviceKey);
        return;
      }
      if (payload.equals("online") || payload.equals("offline")) {
        log.info("Device is {}: {}", payload, deviceKey);
        return;
      }
      if (!payload.trim().startsWith("{")) {
        log.warn("Received non-JSON payload for deviceKey {}: {}", deviceKey, payload);
        return;
      }
      JsonNode root = objectMapper.readTree(payload);
      String event = root.path("event").asText();
      if ("secret_rotated".equals(event)) {
        eventPublisher.publishEvent(new SecretRotatedEvent(deviceKey, payload));
      }
    } catch (Exception e) {
      log.error("Error processing status message: {}", payload, e);
    }
  }

  private String extractDeviceKey(String topic) {
    if (topic == null) return null;

    // Remove leading slash if present
    String normalized = topic.startsWith("/") ? topic.substring(1) : topic;

    if (!normalized.startsWith("hydro/") || !normalized.endsWith("/status")) {
      return null;
    }

    // Remove "hydro/" prefix and "/status" suffix
    String middle = normalized.substring(6); // length of "hydro/"
    if (middle.endsWith("/status")) {
      return middle.substring(0, middle.length() - 7);
    }
    return null;
  }

  private String getHostname() {
    try {
      return InetAddress.getLocalHost().getHostName().replace('.', '-');
    } catch (UnknownHostException e) {
      log.warn("Cannot resolve hostname, using 'unknown'");
      return "unknown";
    }
  }
}