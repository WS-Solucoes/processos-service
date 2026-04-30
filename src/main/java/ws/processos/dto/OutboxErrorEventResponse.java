package ws.processos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ws.processos.event.ProcessoOutboxEvent;

import java.time.LocalDateTime;

/**
 * Representa um evento de outbox em estado de erro, exposto pelo dashboard
 * para diagnóstico e retentativa manual (S5.10).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxErrorEventResponse {

    private Long id;
    private String eventId;
    private Long aggregateId;
    private String aggregateType;
    private String eventType;
    private String routingKey;
    private Integer attempts;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private String status;

    public static OutboxErrorEventResponse from(ProcessoOutboxEvent e) {
        return OutboxErrorEventResponse.builder()
                .id(e.getId())
                .eventId(e.getEventId())
                .aggregateId(e.getAggregateId())
                .aggregateType(e.getAggregateType())
                .eventType(e.getEventType())
                .routingKey(e.getRoutingKey())
                .attempts(e.getAttempts())
                .lastError(e.getLastError())
                .createdAt(e.getCreatedAt())
                .publishedAt(e.getPublishedAt())
                .status(e.getStatus() != null ? e.getStatus().name() : null)
                .build();
    }
}
