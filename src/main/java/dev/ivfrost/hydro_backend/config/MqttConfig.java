package dev.ivfrost.hydro_backend.config;

import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;

@RequiredArgsConstructor
@Configuration
public class MqttConfig {

  private final MqttProperties mqttProperties;
  private final ApiProperties apiProperties;

  @Bean
  public MqttPahoClientFactory mqttClientFactory() {
    DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
    MqttConnectOptions options = new MqttConnectOptions();
    options.setServerURIs(new String[]{mqttProperties.brokerUrl()});
    options.setCleanSession(true);
    options.setUserName(apiProperties.mqttUsername());
    options.setPassword(apiProperties.mqttPassword().toCharArray());
    factory.setConnectionOptions(options);
    return factory;
  }

}

