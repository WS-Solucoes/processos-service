package ws.erh.cadastro.processo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ws.common.logger.SalvarLog;
import ws.erh.cadastro.portal.security.PortalJwtExtractor;
import ws.erh.cadastro.processo.dto.ProcessoComplementacaoRespostaRequest;
import ws.erh.cadastro.processo.dto.ProcessoDetalheResponse;
import ws.erh.cadastro.processo.dto.ProcessoDocumentoResponse;
import ws.erh.cadastro.processo.dto.ProcessoHistoricoResponse;
import ws.erh.cadastro.processo.dto.ProcessoMensagemRequest;
import ws.erh.cadastro.processo.dto.ProcessoMensagemResponse;
import ws.erh.cadastro.processo.dto.ProcessoPortalComplementoRequest;
import ws.erh.cadastro.processo.dto.ProcessoPortalSubmissaoRequest;
import ws.erh.cadastro.processo.dto.ProcessoResponse;
import ws.erh.cadastro.processo.service.ProcessoDetalheService;
import ws.erh.cadastro.processo.service.ProcessoPortalService;
import ws.erh.core.exception.EntityNotFoundException;
import ws.erh.core.tenant.FiltroTenant;
import ws.erh.model.cadastro.processo.Processo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "https://wsolucoes.com", "https://www.wsolucoes.com"})
@RestController
@RequestMapping("/api/v1/processo/portal")
public class ProcessoPortalController {

    @Autowired
    private ProcessoPortalService processoPortalService;

    @Autowired
    private ProcessoDetalheService processoDetalheService;

    @Autowired
    private PortalJwtExtractor portalJwtExtractor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager entityManager;

    @FiltroTenant
    @GetMapping("meus")
    public Page<ProcessoResponse> getMeusProcessos(HttpServletRequest request,
                                                   @PageableDefault(size = 20)
                                                   @SortDefault(sort = "dataAbertura", direction = Direction.DESC)
                                                   Pageable pageable) {
        enableFilters();
        Long servidorId = portalJwtExtractor.extrairServidorId(request);
        Page<Processo> page = processoPortalService.getMeusProcessos(servidorId, pageable);
        List<ProcessoResponse> content = page.getContent().stream()
                .map(ProcessoResponse::new)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @FiltroTenant
    @GetMapping("{id}")
    public ProcessoResponse getMeuProcesso(HttpServletRequest request, @PathVariable Long id) {
        enableFilters();
        Long servidorId = portalJwtExtractor.extrairServidorId(request);
        return new ProcessoResponse(processoPortalService.getMeuProcesso(id, servidorId), true);
    }

    @FiltroTenant
    @GetMapping("{id}/detalhe")
    public ProcessoDetalheResponse getMeuProcessoDetalhe(HttpServletRequest request, @PathVariable Long id) {
        enableFilters();
        Long servidorId = portalJwtExtractor.extrairServidorId(request);
        Processo processo = processoPortalService.getMeuProcesso(id, servidorId);
        return processoDetalheService.build(processo);
    }

    @SalvarLog(acao = "Submeteu processo via portal")
    @PostMapping(value = "submeter", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProcessoResponse submeter(HttpServletRequest request,
                                     @RequestPart("payload") String payload,
                                     @RequestPart(value = "arquivos", required = false) List<MultipartFile> arquivos,
                                     @RequestParam(value = "documentoModeloIds", required = false) List<Long> documentoModeloIds,
                                     @RequestParam(value = "enviadoPor", required = false) String enviadoPor) {
        Long servidorId = portalJwtExtractor.extrairServidorId(request);
        try {
            ProcessoPortalSubmissaoRequest submitRequest = objectMapper.readValue(payload, ProcessoPortalSubmissaoRequest.class);
            Processo processo = processoPortalService.submeter(
                    servidorId,
                    submitRequest,
                    arquivos,
                    documentoModeloIds,
                    enviadoPor != null ? enviadoPor : "Servidor");
            return new ProcessoResponse(processo, true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Complementou processo via portal")
    @PostMapping(value = "{id}/complementar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProcessoResponse complementar(HttpServletRequest request,
                                         @PathVariable Long id,
                                         @RequestPart("payload") String payload,
                                         @RequestPart(value = "arquivos", required = false) List<MultipartFile> arquivos,
                                         @RequestParam(value = "documentoModeloIds", required = false) List<Long> documentoModeloIds,
                                         @RequestParam(value = "enviadoPor", required = false) String enviadoPor) {
        Long servidorId = portalJwtExtractor.extrairServidorId(request);
        try {
            ProcessoPortalComplementoRequest complementoRequest = objectMapper.readValue(payload, ProcessoPortalComplementoRequest.class);
            Processo processo = processoPortalService.complementar(
                    id,
                    servidorId,
                    complementoRequest,
                    arquivos,
                    documentoModeloIds,
                    enviadoPor != null ? enviadoPor : "Servidor");
            return new ProcessoResponse(processo, true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Respondeu complementacao via portal")
    @PostMapping(value = "{id}/complementacoes/{complementacaoId}/responder", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProcessoDetalheResponse responderComplementacao(HttpServletRequest request,
                                                           @PathVariable Long id,
                                                           @PathVariable Long complementacaoId,
                                                           @RequestPart("payload") String payload,
                                                           @RequestPart(value = "arquivos", required = false) List<MultipartFile> arquivos,
                                                           @RequestParam(value = "enviadoPor", required = false) String enviadoPor) {
        Long servidorId = portalJwtExtractor.extrairServidorId(request);
        try {
            ProcessoComplementacaoRespostaRequest respostaRequest =
                    objectMapper.readValue(payload, ProcessoComplementacaoRespostaRequest.class);
            Processo processo = processoPortalService.responderComplementacao(
                    id,
                    servidorId,
                    complementacaoId,
                    respostaRequest,
                    arquivos,
                    enviadoPor != null ? enviadoPor : "Servidor");
            return processoDetalheService.build(processo);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Cancelou processo via portal")
    @PutMapping("{id}/cancelar")
    public ProcessoResponse cancelar(HttpServletRequest request,
                                     @PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body) {
        Long servidorId = portalJwtExtractor.extrairServidorId(request);
        try {
            String justificativa = body != null ? body.get("justificativa") : null;
            String usuario = body != null ? body.get("usuario") : "Servidor";
            return new ProcessoResponse(processoPortalService.cancelar(id, servidorId, justificativa, usuario), true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @FiltroTenant
    @GetMapping("{id}/documentos")
    public List<ProcessoDocumentoResponse> getDocumentos(HttpServletRequest request, @PathVariable Long id) {
        enableFilters();
        Long servidorId = portalJwtExtractor.extrairServidorId(request);
        return processoPortalService.getDocumentos(id, servidorId).stream()
                .map(ProcessoDocumentoResponse::new)
                .collect(Collectors.toList());
    }

    @FiltroTenant
    @GetMapping("documentos/{documentoId}/download")
    public ResponseEntity<byte[]> downloadDocumento(HttpServletRequest request, @PathVariable Long documentoId) {
        enableFilters();
        Long servidorId = portalJwtExtractor.extrairServidorId(request);
        try {
            ProcessoDocumentoResponse documento = new ProcessoDocumentoResponse(
                    processoPortalService.getDocumento(documentoId, servidorId));
            byte[] conteudo = processoPortalService.downloadDocumento(documentoId, servidorId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + documento.getNomeArquivo() + "\"")
                    .contentType(MediaType.parseMediaType(documento.getTipoArquivo() != null ? documento.getTipoArquivo() : "application/octet-stream"))
                    .body(conteudo);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @FiltroTenant
    @GetMapping("documentos/{documentoId}/preview")
    public ResponseEntity<byte[]> previewDocumento(HttpServletRequest request, @PathVariable Long documentoId) {
        enableFilters();
        Long servidorId = portalJwtExtractor.extrairServidorId(request);
        try {
            ProcessoDocumentoResponse documento = new ProcessoDocumentoResponse(
                    processoPortalService.getDocumento(documentoId, servidorId));
            byte[] conteudo = processoPortalService.downloadDocumento(documentoId, servidorId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + documento.getNomeArquivo() + "\"")
                    .contentType(MediaType.parseMediaType(documento.getTipoArquivo() != null ? documento.getTipoArquivo() : "application/octet-stream"))
                    .body(conteudo);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @FiltroTenant
    @GetMapping("{id}/mensagens")
    public List<ProcessoMensagemResponse> getMensagens(HttpServletRequest request, @PathVariable Long id) {
        enableFilters();
        Long servidorId = portalJwtExtractor.extrairServidorId(request);
        return processoPortalService.getMensagens(id, servidorId).stream()
                .map(ProcessoMensagemResponse::new)
                .collect(Collectors.toList());
    }

    @SalvarLog(acao = "Enviou mensagem via portal")
    @PostMapping("{id}/mensagens")
    public ProcessoMensagemResponse enviarMensagem(HttpServletRequest request,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody ProcessoMensagemRequest requestBody) {
        Long servidorId = portalJwtExtractor.extrairServidorId(request);
        try {
            return new ProcessoMensagemResponse(processoPortalService.enviarMensagem(id, servidorId, requestBody));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PutMapping("{id}/mensagens/lidas")
    public String marcarMensagensComoLidas(HttpServletRequest request, @PathVariable Long id) {
        Long servidorId = portalJwtExtractor.extrairServidorId(request);
        processoPortalService.marcarMensagensComoLidas(id, servidorId);
        return "Mensagens marcadas como lidas";
    }

    @FiltroTenant
    @GetMapping("{id}/historico")
    public List<ProcessoHistoricoResponse> getHistorico(HttpServletRequest request, @PathVariable Long id) {
        enableFilters();
        Long servidorId = portalJwtExtractor.extrairServidorId(request);
        return processoPortalService.getHistorico(id, servidorId).stream()
                .map(ProcessoHistoricoResponse::new)
                .collect(Collectors.toList());
    }

    private void enableFilters() {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("excluidoFilter").setParameter("excluido", false);
    }
}
