package dev.ivfrost.hydro_backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmqpConfig {

  public static final String HYDRO_Q = "hydro_q";

  @Bean
  Exchange exchange() {
    return ExchangeBuilder.directExchange(HYDRO_Q).build();
  }

  @Bean
  Queue queue() {
    return QueueBuilder.durable(HYDRO_Q).build();
  }

  @Bean
  Binding binding(Queue queue, Exchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(HYDRO_Q).noargs();
  }
}
