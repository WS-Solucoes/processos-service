package ws.processos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ws.processos.dto.OutboxDashboardResponse;
import ws.processos.dto.OutboxErrorEventResponse;
import ws.processos.event.ProcessoOutboxEvent;
import ws.processos.event.ProcessoOutboxEventRepository;
import ws.processos.event.ProcessoOutboxStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * S5.10 — Dashboard operacional do Outbox + retentativa manual.
 *
 * <p>Expõe métricas (contagens por status, idade do PENDENTE/ERRO mais antigo,
 * SLA estourado, top tipos com erro, amostra de erros recentes) e operações
 * administrativas (reenviar evento individual / reenviar todos os erros).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessoOutboxDashboardService {

    private final ProcessoOutboxEventRepository repository;

    /** SLA padrão (em minutos) para o PENDENTE mais antigo — alerta se ultrapassado. */
    @Value("${processos.outbox.sla-minutos:10}")
    private int slaMinutosDefault;

    /** Quantidade de erros recentes incluídos no dashboard (amostra). */
    @Value("${processos.outbox.dashboard-amostra-erros:10}")
    private int amostraErros;

    @Transactional(readOnly = true)
    public OutboxDashboardResponse getDashboard(Integer slaMinutosOverride) {
        int sla = slaMinutosOverride != null && slaMinutosOverride > 0 ? slaMinutosOverride : slaMinutosDefault;
        LocalDateTime agora = LocalDateTime.now();

        long totalPending = repository.countByStatus(ProcessoOutboxStatus.PENDING);
        long totalPublished = repository.countByStatus(ProcessoOutboxStatus.PUBLISHED);
        long totalError = repository.countByStatus(ProcessoOutboxStatus.ERROR);

        Optional<LocalDateTime> oldestPending = repository
                .findOldestCreatedAtByStatus(ProcessoOutboxStatus.PENDING);
        Optional<LocalDateTime> oldestError = repository
                .findOldestCreatedAtByStatus(ProcessoOutboxStatus.ERROR);

        Long agePending = oldestPending.map(t -> Duration.between(t, agora).toMinutes()).orElse(null);
        Long ageError = oldestError.map(t -> Duration.between(t, agora).toMinutes()).orElse(null);
        boolean slaBreached = agePending != null && agePending > sla;

        List<OutboxDashboardResponse.TipoErroResumo> top = new ArrayList<>();
        for (Object[] row : repository.contarErrosPorTipo(ProcessoOutboxStatus.ERROR)) {
            top.add(OutboxDashboardResponse.TipoErroResumo.builder()
                    .aggregateType((String) row[0])
                    .eventType((String) row[1])
                    .count(((Number) row[2]).longValue())
                    .build());
        }

        List<OutboxErrorEventResponse> errosRecentes = repository
                .findByStatusOrderByCreatedAtDesc(ProcessoOutboxStatus.ERROR,
                        PageRequest.of(0, amostraErros))
                .stream()
                .map(OutboxErrorEventResponse::from)
                .toList();

        return OutboxDashboardResponse.builder()
                .totalPending(totalPending)
                .totalPublished(totalPublished)
                .totalError(totalError)
                .oldestPendingCreatedAt(oldestPending.orElse(null))
                .oldestErrorCreatedAt(oldestError.orElse(null))
                .oldestPendingAgeMinutes(agePending)
                .oldestErrorAgeMinutes(ageError)
                .slaMinutos(sla)
                .slaBreached(slaBreached)
                .topErros(top)
                .errosRecentes(errosRecentes)
                .build();
    }

    @Transactional(readOnly = true)
    public List<OutboxErrorEventResponse> listarErros(int limit) {
        int safeLimit = limit <= 0 ? 50 : Math.min(limit, 500);
        return repository.findByStatusOrderByCreatedAtDesc(ProcessoOutboxStatus.ERROR,
                        PageRequest.of(0, safeLimit))
                .stream()
                .map(OutboxErrorEventResponse::from)
                .toList();
    }

    /**
     * Recoloca um evento em ERROR de volta na fila PENDING para nova tentativa.
     */
    @Transactional
    public OutboxErrorEventResponse reenviar(Long id) {
        ProcessoOutboxEvent event = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Evento de outbox não encontrado: " + id));
        if (event.getStatus() != ProcessoOutboxStatus.ERROR) {
            throw new IllegalStateException(
                    "Apenas eventos com status ERROR podem ser reenviados. Status atual: " + event.getStatus());
        }
        event.setStatus(ProcessoOutboxStatus.PENDING);
        event.setLastError(null);
        repository.save(event);
        log.info("S5.10: evento outbox id={} (eventId={}) reagendado para PENDING (tentativa #{} -> retry manual)",
                event.getId(), event.getEventId(), event.getAttempts());
        return OutboxErrorEventResponse.from(event);
    }

    /**
     * Reagenda todos os eventos em ERROR para PENDING.
     *
     * @return quantidade de eventos reagendados
     */
    @Transactional
    public int reenviarTodos() {
        int total = 0;
        List<ProcessoOutboxEvent> erros = repository
                .findByStatusOrderByCreatedAtDesc(ProcessoOutboxStatus.ERROR, PageRequest.of(0, 1000));
        for (ProcessoOutboxEvent event : erros) {
            event.setStatus(ProcessoOutboxStatus.PENDING);
            event.setLastError(null);
            repository.save(event);
            total++;
        }
        log.info("S5.10: {} eventos reagendados para PENDING (retry em massa)", total);
        return total;
    }
}
