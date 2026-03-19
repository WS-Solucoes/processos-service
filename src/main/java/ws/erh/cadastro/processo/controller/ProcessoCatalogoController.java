package ws.erh.cadastro.processo.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ws.erh.cadastro.processo.dto.ProcessoModeloResponse;
import ws.erh.cadastro.processo.service.ProcessoModeloServiceInterface;
import ws.erh.core.tenant.FiltroTenant;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller público do catálogo de processos visível no portal do servidor.
 */
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "https://wsolucoes.com", "https://www.wsolucoes.com"})
@RestController
@RequestMapping("/api/v1/")
public class ProcessoCatalogoController {

    @Autowired
    private ProcessoModeloServiceInterface processoModeloService;

    @PersistenceContext
    private EntityManager entityManager;

    @FiltroTenant
    @GetMapping("processo/catalogo")
    public List<ProcessoModeloResponse> getCatalogo() {
        enableFilters();
        return processoModeloService.findVisivelPortal().stream()
                .map(ProcessoModeloResponse::new)
                .collect(Collectors.toList());
    }

    @FiltroTenant
    @GetMapping("processo/catalogo/{id}")
    public ProcessoModeloResponse getCatalogoItem(@PathVariable Long id) {
        enableFilters();
        return new ProcessoModeloResponse(processoModeloService.findById(id));
    }

    private void enableFilters() {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("excluidoFilter").setParameter("excluido", false);
    }
}
