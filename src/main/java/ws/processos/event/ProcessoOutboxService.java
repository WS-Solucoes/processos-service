package ws.processos.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ws.common.events.processo.ProcessoIntegracaoSolicitadaEvent;
import ws.common.events.processo.ProcessoLifecycleEvent;

@Service
@RequiredArgsConstructor
public class ProcessoOutboxService {

    private final ProcessoOutboxEventRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${processos.rabbit.lifecycle-routing-key}")
    private String lifecycleRoutingKey;

    @Value("${processos.rabbit.integracao-routing-key}")
    private String integracaoRoutingKey;

    @Transactional
    public void enqueueLifecycle(Long processoId, ProcessoLifecycleEvent event) {
        enqueue(processoId, "PROCESSO", "ProcessoLifecycleEvent", lifecycleRoutingKey, event.getEventId(), event);
    }

    @Transactional
    public void enqueueIntegracaoSolicitada(Long processoId, ProcessoIntegracaoSolicitadaEvent event) {
        enqueue(processoId, "PROCESSO", "ProcessoIntegracaoSolicitadaEvent", integracaoRoutingKey, event.getEventId(), event);
    }

    @Transactional
    public void enqueue(Long aggregateId,
                        String aggregateType,
                        String eventType,
                        String routingKey,
                        String eventId,
                        Object payloadObject) {
        try {
            ProcessoOutboxEvent outbox = new ProcessoOutboxEvent();
            outbox.setAggregateId(aggregateId);
            outbox.setAggregateType(aggregateType);
            outbox.setEventType(eventType);
            outbox.setRoutingKey(routingKey);
            outbox.setEventId(eventId);
            outbox.setPayload(objectMapper.writeValueAsString(payloadObject));
            repository.save(outbox);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar evento de processo.", e);
        }
    }
}
