package ws.processos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resposta do dashboard de Outbox do processos-service (S5.10).
 *
 * <p>Inclui contagens por status, métricas de SLA (idade do PENDENTE mais antigo,
 * idade do ERRO mais antigo) e amostra dos erros mais recentes.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxDashboardResponse {

    private long totalPending;
    private long totalPublished;
    private long totalError;

    private LocalDateTime oldestPendingCreatedAt;
    private LocalDateTime oldestErrorCreatedAt;

    /** Idade em minutos do PENDENTE mais antigo (null se não há pendentes). */
    private Long oldestPendingAgeMinutes;
    /** Idade em minutos do ERRO mais antigo (null se não há erros). */
    private Long oldestErrorAgeMinutes;

    /** SLA configurado (em minutos) usado para calcular {@link #slaBreached}. */
    private Integer slaMinutos;
    /** {@code true} quando há PENDENTE mais velho que o SLA. */
    private boolean slaBreached;

    /** Top tipos de evento com erro (aggregateType + eventType + count). */
    private List<TipoErroResumo> topErros;

    /** Amostra dos erros mais recentes para diagnóstico rápido. */
    private List<OutboxErrorEventResponse> errosRecentes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TipoErroResumo {
        private String aggregateType;
        private String eventType;
        private long count;
    }
}
