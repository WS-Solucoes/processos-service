package ws.erh.cadastro.processo.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ws.common.logger.SalvarLog;
import ws.erh.cadastro.processo.dto.ProcessoDetalheResponse;
import ws.erh.cadastro.processo.dto.ProcessoDocumentoResponse;
import ws.erh.cadastro.processo.dto.ProcessoHistoricoResponse;
import ws.erh.cadastro.processo.dto.ProcessoMensagemRequest;
import ws.erh.cadastro.processo.dto.ProcessoMensagemResponse;
import ws.erh.cadastro.processo.dto.ProcessoRequest;
import ws.erh.cadastro.processo.dto.ProcessoResponse;
import ws.erh.cadastro.processo.service.ProcessoDetalheService;
import ws.erh.cadastro.processo.service.ProcessoDocumentoServiceInterface;
import ws.erh.cadastro.processo.service.ProcessoMensagemServiceInterface;
import ws.erh.cadastro.processo.service.ProcessoModeloServiceInterface;
import ws.erh.cadastro.processo.service.ProcessoServiceInterface;
import ws.erh.core.enums.processo.Prioridade;
import ws.erh.core.enums.processo.TipoAutor;
import ws.erh.core.exception.EntityNotFoundException;
import ws.erh.core.tenant.FiltroTenant;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoDocumento;
import ws.erh.model.cadastro.processo.ProcessoMensagem;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "https://wsolucoes.com", "https://www.wsolucoes.com"})
@RestController
@RequestMapping("/api/v1/")
public class ProcessoController {

    @Autowired
    private ProcessoServiceInterface processoService;

    @Autowired
    private ProcessoModeloServiceInterface processoModeloService;

    @Autowired
    private ProcessoDocumentoServiceInterface documentoService;

    @Autowired
    private ProcessoMensagemServiceInterface mensagemService;

    @Autowired
    private ProcessoDetalheService processoDetalheService;

    @PersistenceContext
    private EntityManager entityManager;

    @SalvarLog(acao = "Abriu processo")
    @PostMapping("processo")
    public ProcessoResponse abrirProcesso(@Valid @RequestBody ProcessoRequest request) {
        try {
            Processo processo = new Processo();
            ProcessoModelo modelo = processoModeloService.findById(request.getProcessoModeloId());
            processo.setProcessoModelo(modelo);
            processo.setServidorId(request.getServidorId());

            if (request.getVinculoFuncionalId() != null) {
                processo.setVinculoFuncionalId(request.getVinculoFuncionalId());
            }

            processo.setDadosFormulario(request.getDadosFormulario());
            processo.setObservacaoServidor(request.getObservacaoServidor());
            if (request.getPrioridade() != null) {
                processo.setPrioridade(Prioridade.valueOf(request.getPrioridade()));
            }

            Processo saved = processoService.abrirProcesso(processo);
            return new ProcessoResponse(processoService.findById(saved.getId()), true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @FiltroTenant
    @GetMapping("processo")
    public Page<ProcessoResponse> getAllProcessos(@PageableDefault(size = 20)
                                                  @SortDefault(sort = "dataAbertura", direction = Direction.DESC)
                                                  Pageable pageable) {
        enableFilters();
        Page<Processo> page = processoService.getAll(pageable);
        List<ProcessoResponse> content = page.getContent().stream()
                .map(ProcessoResponse::new)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @FiltroTenant
    @GetMapping("processo/{id}")
    public ProcessoResponse getProcessoById(@PathVariable Long id) {
        enableFilters();
        try {
            return new ProcessoResponse(processoService.findById(id), true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @FiltroTenant
    @GetMapping("processo/{id}/detalhe")
    public ProcessoDetalheResponse getProcessoDetalhe(@PathVariable Long id) {
        enableFilters();
        try {
            return processoDetalheService.build(processoService.findById(id));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @FiltroTenant
    @GetMapping("processo/protocolo/{protocolo}")
    public ProcessoResponse getProcessoByProtocolo(@PathVariable String protocolo) {
        enableFilters();
        try {
            return new ProcessoResponse(processoService.findByProtocolo(protocolo), true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @FiltroTenant
    @GetMapping("processo/servidor/{servidorId}")
    public Page<ProcessoResponse> getProcessosByServidor(@PathVariable Long servidorId,
                                                         @PageableDefault(size = 20)
                                                         @SortDefault(sort = "dataAbertura", direction = Direction.DESC)
                                                         Pageable pageable) {
        enableFilters();
        Page<Processo> page = processoService.findByServidorId(servidorId, pageable);
        List<ProcessoResponse> content = page.getContent().stream()
                .map(ProcessoResponse::new)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @SalvarLog(acao = "Cancelou processo")
    @PutMapping("processo/{id}/cancelar")
    public ProcessoResponse cancelarProcesso(@PathVariable Long id,
                                             @RequestBody(required = false) Map<String, String> body) {
        try {
            String justificativa = body != null ? body.get("justificativa") : null;
            String usuario = body != null ? body.get("usuario") : "Servidor";
            return new ProcessoResponse(processoService.cancelar(id, justificativa, usuario), true);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Excluiu processo")
    @DeleteMapping("processo/{id}")
    public String deleteProcesso(@PathVariable Long id) {
        try {
            processoService.deleteProcesso(id);
            return "Processo excluído com sucesso";
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Enviou documento ao processo")
    @PostMapping(value = "processo/{processoId}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProcessoDocumentoResponse uploadDocumento(@PathVariable Long processoId,
                                                     @RequestParam("arquivo") MultipartFile arquivo,
                                                     @RequestParam(value = "documentoModeloId", required = false) Long documentoModeloId,
                                                     @RequestParam(value = "enviadoPor") String enviadoPor) {
        try {
            Processo processo = processoService.findById(processoId);
            return new ProcessoDocumentoResponse(
                    documentoService.uploadDocumento(processo, arquivo, documentoModeloId, enviadoPor));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @FiltroTenant
    @GetMapping("processo/{processoId}/documentos")
    public List<ProcessoDocumentoResponse> getDocumentos(@PathVariable Long processoId) {
        enableFilters();
        return documentoService.findByProcessoId(processoId).stream()
                .map(ProcessoDocumentoResponse::new)
                .collect(Collectors.toList());
    }

    @FiltroTenant
    @GetMapping("processo/documentos/{documentoId}/download")
    public ResponseEntity<byte[]> downloadDocumento(@PathVariable Long documentoId) {
        enableFilters();
        try {
            ProcessoDocumento doc = documentoService.findById(documentoId);
            byte[] conteudo = documentoService.downloadDocumento(documentoId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getNomeArquivo() + "\"")
                    .contentType(MediaType.parseMediaType(doc.getTipoArquivo() != null ? doc.getTipoArquivo() : "application/octet-stream"))
                    .body(conteudo);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @FiltroTenant
    @GetMapping("processo/documentos/{documentoId}/preview")
    public ResponseEntity<byte[]> previewDocumento(@PathVariable Long documentoId) {
        enableFilters();
        try {
            ProcessoDocumento doc = documentoService.findById(documentoId);
            byte[] conteudo = documentoService.downloadDocumento(documentoId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getNomeArquivo() + "\"")
                    .contentType(MediaType.parseMediaType(doc.getTipoArquivo() != null ? doc.getTipoArquivo() : "application/octet-stream"))
                    .body(conteudo);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @SalvarLog(acao = "Enviou mensagem no processo")
    @PostMapping("processo/{processoId}/mensagens")
    public ProcessoMensagemResponse enviarMensagem(@PathVariable Long processoId,
                                                   @Valid @RequestBody ProcessoMensagemRequest request) {
        try {
            Processo processo = processoService.findById(processoId);
            ProcessoMensagem mensagem = new ProcessoMensagem();
            mensagem.setProcesso(processo);
            mensagem.setAutor(request.getAutor());
            mensagem.setTipoAutor(TipoAutor.valueOf(request.getTipoAutor()));
            mensagem.setMensagem(request.getMensagem());
            return new ProcessoMensagemResponse(mensagemService.enviarMensagem(mensagem));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @FiltroTenant
    @GetMapping("processo/{processoId}/mensagens")
    public List<ProcessoMensagemResponse> getMensagens(@PathVariable Long processoId) {
        enableFilters();
        return mensagemService.findByProcessoId(processoId).stream()
                .map(ProcessoMensagemResponse::new)
                .collect(Collectors.toList());
    }

    @PutMapping("processo/{processoId}/mensagens/lidas")
    public String marcarMensagensComoLidas(@PathVariable Long processoId) {
        mensagemService.marcarComoLidas(processoId);
        return "Mensagens marcadas como lidas";
    }

    @FiltroTenant
    @GetMapping("processo/{processoId}/historico")
    public List<ProcessoHistoricoResponse> getHistorico(@PathVariable Long processoId) {
        enableFilters();
        Processo processo = processoService.findById(processoId);
        return processo.getHistorico().stream()
                .map(ProcessoHistoricoResponse::new)
                .collect(Collectors.toList());
    }

    @FiltroTenant
    @GetMapping("processo/dashboard")
    public Map<String, Object> getDashboard() {
        enableFilters();
        return processoService.getDashboard();
    }

    private void enableFilters() {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("excluidoFilter").setParameter("excluido", false);
    }
}
