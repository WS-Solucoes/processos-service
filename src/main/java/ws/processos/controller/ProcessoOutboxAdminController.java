package ws.processos.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ws.processos.dto.OutboxDashboardResponse;
import ws.processos.dto.OutboxErrorEventResponse;
import ws.processos.service.ProcessoOutboxDashboardService;

import java.util.List;
import java.util.Map;

/**
 * S5.10 — Endpoints de operação do Outbox.
 *
 * <ul>
 *   <li>{@code GET  /api/v1/processos/outbox/dashboard} — métricas + SLA</li>
 *   <li>{@code GET  /api/v1/processos/outbox/erros}     — listagem de erros</li>
 *   <li>{@code POST /api/v1/processos/outbox/erros/{id}/retry}     — retry individual</li>
 *   <li>{@code POST /api/v1/processos/outbox/erros/retry-all}      — retry em massa</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/processos/outbox")
@RequiredArgsConstructor
public class ProcessoOutboxAdminController {

    private final ProcessoOutboxDashboardService dashboardService;

    @GetMapping("/dashboard")
    public OutboxDashboardResponse getDashboard(
            @RequestParam(value = "slaMinutos", required = false) Integer slaMinutos) {
        return dashboardService.getDashboard(slaMinutos);
    }

    @GetMapping("/erros")
    public List<OutboxErrorEventResponse> listarErros(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return dashboardService.listarErros(limit);
    }

    @PostMapping("/erros/{id}/retry")
    public OutboxErrorEventResponse retry(@PathVariable("id") Long id) {
        return dashboardService.reenviar(id);
    }

    @PostMapping("/erros/retry-all")
    public Map<String, Object> retryAll() {
        int total = dashboardService.reenviarTodos();
        return Map.of("reagendados", total);
    }
}
