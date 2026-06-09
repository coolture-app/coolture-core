package pl.coolture.restapi.embedding.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.coolture.restapi.common.config.messaging.RabbitMQConfig;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostEmbeddingPublisher {

  private final RabbitTemplate rabbitTemplate;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publish(PostEmbeddingMessage message) {
    log.info("Attempting to publish embedding message for post {}", message.postId());
    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EMBEDDING_EXCHANGE, RabbitMQConfig.EMBEDDING_ROUTING_KEY, message);
      log.info("Published embedding message for post {}", message.postId());
    } catch (Exception e) {
      log.warn("Failed to publish embedding message for post {}: {}", message.postId(), e.getMessage());
    }
  }
}
