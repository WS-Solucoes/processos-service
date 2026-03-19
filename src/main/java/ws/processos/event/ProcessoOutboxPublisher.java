package ws.processos.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessoOutboxPublisher {

    private final ProcessoOutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${processos.rabbit.exchange}")
    private String exchange;

    @Scheduled(fixedDelayString = "${processos.rabbit.outbox-delay-ms}")
    @Transactional
    public void publishPending() {
        List<ProcessoOutboxEvent> events = repository.findTop50ByStatusOrderByCreatedAtAsc(ProcessoOutboxStatus.PENDING);
        for (ProcessoOutboxEvent event : events) {
            try {
                rabbitTemplate.convertAndSend(exchange, event.getRoutingKey(), event.getPayload());
                event.setStatus(ProcessoOutboxStatus.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());
                event.setLastError(null);
            } catch (Exception e) {
                event.setStatus(ProcessoOutboxStatus.ERROR);
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(e.getMessage());
                log.error("Falha ao publicar evento {}: {}", event.getEventId(), e.getMessage());
            }
            repository.save(event);
        }
    }
}
