package ws.processos.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ws.processos.dto.ProcessoResumoResponse;
import ws.processos.service.ProcessoResumoService;

@RestController
@RequestMapping("/api/v1/processo/resumo")
@RequiredArgsConstructor
public class ProcessoResumoController {

    private final ProcessoResumoService processoResumoService;

    @GetMapping("/servidor/{servidorId}")
    public ProcessoResumoResponse getResumo(@PathVariable Long servidorId,
                                            @RequestParam("unidadeGestoraId") Long unidadeGestoraId) {
        return processoResumoService.getResumo(servidorId, unidadeGestoraId);
    }
}
