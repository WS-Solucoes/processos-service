package ws.processos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import ws.processos.dto.OutboxDashboardResponse;
import ws.processos.dto.OutboxErrorEventResponse;
import ws.processos.event.ProcessoOutboxEvent;
import ws.processos.event.ProcessoOutboxEventRepository;
import ws.processos.event.ProcessoOutboxStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessoOutboxDashboardService")
class ProcessoOutboxDashboardServiceTest {

    @Mock private ProcessoOutboxEventRepository repository;

    @InjectMocks private ProcessoOutboxDashboardService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "slaMinutosDefault", 10);
        ReflectionTestUtils.setField(service, "amostraErros", 5);
    }

    private ProcessoOutboxEvent erro(Long id, String aggType, String evType, int attempts, String msg) {
        ProcessoOutboxEvent e = new ProcessoOutboxEvent();
        e.setId(id);
        e.setEventId("evt-" + id);
        e.setAggregateId(100L);
        e.setAggregateType(aggType);
        e.setEventType(evType);
        e.setRoutingKey(aggType + "." + evType);
        e.setStatus(ProcessoOutboxStatus.ERROR);
        e.setAttempts(attempts);
        e.setLastError(msg);
        e.setCreatedAt(LocalDateTime.now().minusMinutes(5L * id));
        return e;
    }

    @Test
    @DisplayName("getDashboard agrega contagens, idade e marca SLA quando estourado")
    void dashboardSlaBreached() {
        when(repository.countByStatus(ProcessoOutboxStatus.PENDING)).thenReturn(3L);
        when(repository.countByStatus(ProcessoOutboxStatus.PUBLISHED)).thenReturn(120L);
        when(repository.countByStatus(ProcessoOutboxStatus.ERROR)).thenReturn(2L);
        // PENDING mais antigo: 30 min atrás (SLA padrão é 10 min)
        when(repository.findOldestCreatedAtByStatus(ProcessoOutboxStatus.PENDING))
                .thenReturn(Optional.of(LocalDateTime.now().minusMinutes(30)));
        when(repository.findOldestCreatedAtByStatus(ProcessoOutboxStatus.ERROR))
                .thenReturn(Optional.of(LocalDateTime.now().minusMinutes(60)));
        when(repository.contarErrosPorTipo(ProcessoOutboxStatus.ERROR))
                .thenReturn(List.<Object[]>of(new Object[]{"Processo", "PROCESSO_DEFERIDO", 2L}));
        when(repository.findByStatusOrderByCreatedAtDesc(eq(ProcessoOutboxStatus.ERROR), any(Pageable.class)))
                .thenReturn(List.of(erro(1L, "Processo", "PROCESSO_DEFERIDO", 1, "RabbitMQ down")));

        OutboxDashboardResponse dash = service.getDashboard(null);

        assertEquals(3L, dash.getTotalPending());
        assertEquals(120L, dash.getTotalPublished());
        assertEquals(2L, dash.getTotalError());
        assertNotNull(dash.getOldestPendingAgeMinutes());
        assertTrue(dash.getOldestPendingAgeMinutes() >= 29);
        assertTrue(dash.isSlaBreached());
        assertEquals(10, dash.getSlaMinutos());
        assertEquals(1, dash.getTopErros().size());
        assertEquals("Processo", dash.getTopErros().get(0).getAggregateType());
        assertEquals(1, dash.getErrosRecentes().size());
    }

    @Test
    @DisplayName("getDashboard sem pendentes não marca SLA estourado")
    void dashboardSemPendentes() {
        when(repository.countByStatus(ProcessoOutboxStatus.PENDING)).thenReturn(0L);
        when(repository.countByStatus(ProcessoOutboxStatus.PUBLISHED)).thenReturn(50L);
        when(repository.countByStatus(ProcessoOutboxStatus.ERROR)).thenReturn(0L);
        when(repository.findOldestCreatedAtByStatus(ProcessoOutboxStatus.PENDING))
                .thenReturn(Optional.empty());
        when(repository.findOldestCreatedAtByStatus(ProcessoOutboxStatus.ERROR))
                .thenReturn(Optional.empty());
        when(repository.contarErrosPorTipo(ProcessoOutboxStatus.ERROR)).thenReturn(List.of());
        when(repository.findByStatusOrderByCreatedAtDesc(eq(ProcessoOutboxStatus.ERROR), any(Pageable.class)))
                .thenReturn(List.of());

        OutboxDashboardResponse dash = service.getDashboard(null);

        assertNull(dash.getOldestPendingAgeMinutes());
        assertFalse(dash.isSlaBreached());
        assertTrue(dash.getTopErros().isEmpty());
        assertTrue(dash.getErrosRecentes().isEmpty());
    }

    @Test
    @DisplayName("getDashboard respeita slaMinutosOverride")
    void dashboardOverrideSla() {
        when(repository.countByStatus(any())).thenReturn(0L);
        when(repository.findOldestCreatedAtByStatus(ProcessoOutboxStatus.PENDING))
                .thenReturn(Optional.of(LocalDateTime.now().minusMinutes(40)));
        when(repository.findOldestCreatedAtByStatus(ProcessoOutboxStatus.ERROR))
                .thenReturn(Optional.empty());
        when(repository.contarErrosPorTipo(ProcessoOutboxStatus.ERROR)).thenReturn(List.of());
        when(repository.findByStatusOrderByCreatedAtDesc(eq(ProcessoOutboxStatus.ERROR), any(Pageable.class)))
                .thenReturn(List.of());

        OutboxDashboardResponse dash = service.getDashboard(60);

        assertEquals(60, dash.getSlaMinutos());
        assertFalse(dash.isSlaBreached(), "40 min < 60 min, não deve estourar");
    }

    @Test
    @DisplayName("reenviar move evento ERROR para PENDING e limpa lastError")
    void reenviarEventoErro() {
        ProcessoOutboxEvent e = erro(7L, "Processo", "X", 1, "boom");
        when(repository.findById(7L)).thenReturn(Optional.of(e));

        OutboxErrorEventResponse resp = service.reenviar(7L);

        assertEquals("PENDING", resp.getStatus());
        assertEquals(ProcessoOutboxStatus.PENDING, e.getStatus());
        assertNull(e.getLastError());
        verify(repository).save(e);
    }

    @Test
    @DisplayName("reenviar lança IllegalStateException se status != ERROR")
    void reenviarBloqueiaNaoErro() {
        ProcessoOutboxEvent e = erro(7L, "Processo", "X", 0, null);
        e.setStatus(ProcessoOutboxStatus.PUBLISHED);
        when(repository.findById(7L)).thenReturn(Optional.of(e));

        assertThrows(IllegalStateException.class, () -> service.reenviar(7L));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("reenviar lança IllegalArgumentException quando id não existe")
    void reenviarNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.reenviar(99L));
    }

    @Test
    @DisplayName("reenviarTodos reseta todos os ERROR para PENDING e retorna count")
    void reenviarTodosOk() {
        ProcessoOutboxEvent a = erro(1L, "Processo", "X", 1, "e1");
        ProcessoOutboxEvent b = erro(2L, "Processo", "Y", 2, "e2");
        when(repository.findByStatusOrderByCreatedAtDesc(eq(ProcessoOutboxStatus.ERROR), any(Pageable.class)))
                .thenReturn(List.of(a, b));

        int total = service.reenviarTodos();

        assertEquals(2, total);
        assertEquals(ProcessoOutboxStatus.PENDING, a.getStatus());
        assertEquals(ProcessoOutboxStatus.PENDING, b.getStatus());
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("listarErros aplica limite mínimo (50) quando 0 e máximo (500)")
    void listarErrosLimites() {
        when(repository.findByStatusOrderByCreatedAtDesc(eq(ProcessoOutboxStatus.ERROR), any(Pageable.class)))
                .thenReturn(List.of());

        service.listarErros(0);
        service.listarErros(99999);

        verify(repository, times(2))
                .findByStatusOrderByCreatedAtDesc(eq(ProcessoOutboxStatus.ERROR), any(Pageable.class));
    }
}
