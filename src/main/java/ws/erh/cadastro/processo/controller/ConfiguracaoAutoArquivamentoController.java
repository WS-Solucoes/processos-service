package ws.erh.cadastro.processo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ws.erh.cadastro.processo.service.ConfiguracaoAutoArquivamentoService;
import ws.erh.model.cadastro.processo.ConfiguracaoAutoArquivamento;

/**
 * Endpoints de configuração de auto-arquivamento (S7.10).
 *
 * <ul>
 *   <li>{@code GET /api/v1/processos/config/auto-arquivamento} — config atual</li>
 *   <li>{@code PUT /api/v1/processos/config/auto-arquivamento} — atualizar</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/processos/config/auto-arquivamento")
public class ConfiguracaoAutoArquivamentoController {

    @Autowired
    private ConfiguracaoAutoArquivamentoService service;

    @GetMapping
    public ConfiguracaoAutoArquivamento obter() {
        return service.obterOuCriarPadrao();
    }

    @PutMapping
    public ConfiguracaoAutoArquivamento atualizar(@RequestBody ConfiguracaoAutoArquivamento payload) {
        return service.atualizar(payload);
    }
}
