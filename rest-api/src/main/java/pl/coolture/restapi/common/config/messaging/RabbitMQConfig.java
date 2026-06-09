package pl.coolture.restapi.common.config.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  public static final String EMBEDDING_EXCHANGE    = "embedding.exchange";
  public static final String EMBEDDING_QUEUE       = "post.embedding.queue";
  public static final String EMBEDDING_DLQ         = "post.embedding.dlq";
  public static final String EMBEDDING_ROUTING_KEY = "post.embedding";

  @Bean
  JacksonJsonMessageConverter jsonMessageConverter() {
    return new JacksonJsonMessageConverter();
  }

  @Bean
  RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(jsonMessageConverter());
    return template;
  }

  @Bean
  DirectExchange embeddingExchange() {
    return new DirectExchange(EMBEDDING_EXCHANGE);
  }

  @Bean
  Queue embeddingQueue() {
    return QueueBuilder.durable(EMBEDDING_QUEUE)
        .withArgument("x-dead-letter-exchange", "")
        .withArgument("x-dead-letter-routing-key", EMBEDDING_DLQ)
        .build();
  }

  @Bean
  Queue embeddingDlq() {
    return QueueBuilder.durable(EMBEDDING_DLQ).build();
  }

  @Bean
  Binding embeddingBinding() {
    return BindingBuilder.bind(embeddingQueue()).to(embeddingExchange()).with(EMBEDDING_ROUTING_KEY);
  }
}
