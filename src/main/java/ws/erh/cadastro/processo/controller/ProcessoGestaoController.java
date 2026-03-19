package ws.erh.cadastro.processo.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ws.common.logger.SalvarLog;
import ws.erh.cadastro.processo.dto.ProcessoComplementacaoRequest;
import ws.erh.cadastro.processo.dto.ProcessoDocumentoResponse;
import ws.erh.cadastro.processo.dto.ProcessoGestaoRequest;
import ws.erh.cadastro.processo.dto.ProcessoResponse;
import ws.erh.cadastro.processo.dto.ProcessoSolicitacaoRequest;
import ws.erh.cadastro.processo.service.ProcessoComplementacaoService;
import ws.erh.cadastro.processo.service.ProcessoDocumentoServiceInterface;
import ws.erh.cadastro.processo.service.ProcessoGestaoServiceInterface;
import ws.erh.cadastro.processo.service.ProcessoIntegracaoOrchestratorService;
import ws.erh.cadastro.processo.service.ProcessoMensagemServiceInterface;
import ws.erh.cadastro.processo.service.ProcessoModeloServiceInterface;
import ws.erh.cadastro.processo.service.ProcessoServiceInterface;
import ws.erh.core.enums.processo.Prioridade;
import ws.erh.core.enums.processo.TipoAutor;
import ws.erh.core.exception.EntityNotFoundException;
import ws.erh.core.tenant.FiltroTenant;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoMensagem;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "https://wsolucoes.com", "https://www.wsolucoes.com"})
@RestController
@RequestMapping("/api/v1/")
public class ProcessoGestaoController {

    @Autowired
    private ProcessoServiceInterface processoService;

    @Autowired
    private ProcessoModeloServiceInterface processoModeloService;

    @Autowired
    private ProcessoGestaoServiceInterface processoGestaoService;

    @Autowired
    private ProcessoDocumentoServiceInterface documentoService;

    @Autowired
    private ProcessoMensagemServiceInterface mensagemService;

    @Autowired
    private ProcessoIntegracaoOrchestratorService integracaoOrchestratorService;

    @Autowired
    private ProcessoComplementacaoService complementacaoService;

    @PersistenceContext
    private EntityManager entityManager;

    @FiltroTenant
    @GetMapping("processo/gestao/pendentes")
    public Page<ProcessoResponse> getProcessosPendentes(@PageableDefault(size = 20)
                                                        @SortDefault(sort = "dataAbertura", direction = Direction.ASC)
                                                        Pageable pageable) {
        enableFilters();
        Page<Processo> page = processoService.findPendentes(pageable);
        List<ProcessoResponse> content = page.getContent().stream()
                .map(ProcessoResponse::new)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @FiltroTenant
    @GetMapping("processo/gestao/vencidos")
    public List<ProcessoResponse> getProcessosVencidos() {
        enableFilters();
        return processoService.findVencidos().stream()
                .map(ProcessoResponse::new)
                .collect(Collectors.toList());
    }

    @SalvarLog(acao = "Solicitou processo para servidor")
    @PostMapping("processo/gestao/solicitar")
    public ProcessoResponse solicitarProcesso(@RequestBody ProcessoSolicitacaoRequest request) {
        try {
            ProcessoModelo modelo = processoModeloService.findById(request.getProcessoModeloId());

            Processo processo = new Processo();
            processo.setProcessoModelo(modelo);
            processo.setServidorId(request.getServidorId());
            processo.setVinculoFuncionalId(request.getVinculoFuncionalId());
            processo.setDadosFormulario(request.getDadosFormulario());
            processo.setPrioridade(parsePrioridade(request.getPrioridade()));
            if (request.getPrazoLimite() != null && !request.getPrazoLimite().isBlank()) {
                processo.setPrazoLimite(LocalDate.parse(request.getPrazoLimite()));
            }

            Processo saved = processoService.solicitarProcesso(processo, "RH", request.getMensagemInicial());

            if (request.getMensagemInicial() != null && !request.getMensagemInicial().isBlank()) {
                ProcessoMensagem mensagem = new ProcessoMensagem();
                mensagem.setProcesso(saved);
                mensagem.setAutor("RH");
                mensagem.setTipoAutor(TipoAutor.RH);
                mensagem.setMensagem(request.getMensagemInicial());
                mensagemService.enviarMensagem(mensagem);
            }

            return new ProcessoResponse(processoService.findById(saved.getId()), true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Atribuiu processo")
    @PostMapping("processo/gestao/atribuir")
    public ProcessoResponse atribuirProcesso(@RequestBody ProcessoGestaoRequest request) {
        try {
            if (request.getProcessoId() == null) {
                throw new IllegalArgumentException("ID do processo é obrigatório para atribuição");
            }
            return new ProcessoResponse(
                    processoGestaoService.atribuir(
                            request.getProcessoId(),
                            request.getAtribuidoPara(),
                            request.getDepartamentoAtribuido(),
                            request.getUsuario()),
                    true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Iniciou análise de processo")
    @PostMapping("processo/gestao/{id}/analisar")
    public ProcessoResponse iniciarAnalise(@PathVariable Long id,
                                           @RequestBody(required = false) Map<String, String> body) {
        try {
            String usuario = body != null ? body.get("usuario") : "RH";
            return new ProcessoResponse(processoGestaoService.iniciarAnalise(id, usuario), true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Solicitou documentação")
    @PostMapping("processo/gestao/{id}/solicitar-documentacao")
    public ProcessoResponse solicitarDocumentacao(@PathVariable Long id,
                                                  @RequestBody ProcessoGestaoRequest request) {
        try {
            return new ProcessoResponse(
                    processoGestaoService.solicitarDocumentacao(id, request.getDocumentoSolicitado(), request.getUsuario()),
                    true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Criou complementação estruturada")
    @PostMapping("processo/gestao/{id}/complementacoes")
    public ProcessoResponse criarComplementacao(@PathVariable Long id,
                                                @RequestBody ProcessoComplementacaoRequest request) {
        try {
            return new ProcessoResponse(complementacaoService.solicitarComplementacao(id, request, "RH"), true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Encerrrou complementação")
    @PostMapping("processo/gestao/complementacoes/{complementacaoId}/encerrar")
    public String encerrarComplementacao(@PathVariable Long complementacaoId,
                                         @RequestBody(required = false) Map<String, String> body) {
        try {
            String usuario = body != null ? body.get("usuario") : "RH";
            complementacaoService.encerrarComplementacao(complementacaoId, usuario);
            return "Complementação encerrada com sucesso";
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Encaminhou processo para chefia")
    @PostMapping("processo/gestao/{id}/encaminhar-chefia")
    public ProcessoResponse encaminharChefia(@PathVariable Long id,
                                             @RequestBody(required = false) Map<String, String> body) {
        try {
            String usuario = body != null ? body.get("usuario") : "RH";
            return new ProcessoResponse(processoGestaoService.encaminharChefia(id, usuario), true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Deferiu processo")
    @PostMapping("processo/gestao/{id}/deferir")
    public ProcessoResponse deferir(@PathVariable Long id, @RequestBody ProcessoGestaoRequest request) {
        try {
            processoGestaoService.deferir(id, request.getJustificativa(), request.getUsuario());
            integracaoOrchestratorService.executarOuReprocessar(id);
            return new ProcessoResponse(processoService.findById(id), true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Reprocessou integração do processo")
    @PostMapping("processo/gestao/{id}/reprocessar-integracao")
    public ProcessoResponse reprocessarIntegracao(@PathVariable Long id) {
        try {
            integracaoOrchestratorService.executarOuReprocessar(id);
            return new ProcessoResponse(processoService.findById(id), true);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Indeferiu processo")
    @PostMapping("processo/gestao/{id}/indeferir")
    public ProcessoResponse indeferir(@PathVariable Long id, @RequestBody ProcessoGestaoRequest request) {
        try {
            return new ProcessoResponse(
                    processoGestaoService.indeferir(id, request.getJustificativa(), request.getUsuario()),
                    true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Devolveu processo")
    @PostMapping("processo/gestao/{id}/devolver")
    public ProcessoResponse devolver(@PathVariable Long id, @RequestBody ProcessoGestaoRequest request) {
        try {
            return new ProcessoResponse(
                    processoGestaoService.devolver(id, request.getJustificativa(), request.getUsuario()),
                    true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Arquivou processo")
    @PostMapping("processo/gestao/{id}/arquivar")
    public ProcessoResponse arquivar(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body) {
        try {
            String usuario = body != null ? body.get("usuario") : "RH";
            return new ProcessoResponse(processoGestaoService.arquivar(id, usuario), true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Aceitou documento")
    @PostMapping("processo/gestao/documentos/{documentoId}/aceitar")
    public ProcessoDocumentoResponse aceitarDocumento(@PathVariable Long documentoId,
                                                      @RequestBody(required = false) Map<String, String> body) {
        try {
            String avaliadoPor = body != null ? body.get("avaliadoPor") : "RH";
            return new ProcessoDocumentoResponse(documentoService.aceitarDocumento(documentoId, avaliadoPor));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @SalvarLog(acao = "Recusou documento")
    @PostMapping("processo/gestao/documentos/{documentoId}/recusar")
    public ProcessoDocumentoResponse recusarDocumento(@PathVariable Long documentoId,
                                                      @RequestBody Map<String, String> body) {
        try {
            String motivoRecusa = body.get("motivoRecusa");
            String avaliadoPor = body.getOrDefault("avaliadoPor", "RH");
            if (motivoRecusa == null || motivoRecusa.isBlank()) {
                throw new IllegalStateException("Motivo da recusa é obrigatório");
            }

            var documento = documentoService.recusarDocumento(documentoId, motivoRecusa, avaliadoPor);

            ProcessoComplementacaoRequest request = new ProcessoComplementacaoRequest();
            request.setMotivoConsolidado(motivoRecusa);
            ProcessoComplementacaoRequest.ItemRequest item = new ProcessoComplementacaoRequest.ItemRequest();
            item.setTipoItem(documento.getDocumentoModelo() != null ? "DOCUMENTO_MODELO" : "DOCUMENTO_LIVRE");
            item.setDocumentoModeloId(documento.getDocumentoModelo() != null ? documento.getDocumentoModelo().getId() : null);
            item.setLabel(documento.getDocumentoModelo() != null
                    ? documento.getDocumentoModelo().getNome()
                    : documento.getNomeArquivo());
            item.setObrigatorio(true);
            item.setMotivo(motivoRecusa);
            item.setOrdem(1);
            request.getItens().add(item);
            complementacaoService.solicitarComplementacao(documento.getProcesso().getId(), request, avaliadoPor);

            return new ProcessoDocumentoResponse(documento);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private Prioridade parsePrioridade(String prioridade) {
        if (prioridade == null || prioridade.isBlank()) {
            return Prioridade.NORMAL;
        }
        return Prioridade.valueOf(prioridade);
    }

    private void enableFilters() {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("excluidoFilter").setParameter("excluido", false);
    }
}
