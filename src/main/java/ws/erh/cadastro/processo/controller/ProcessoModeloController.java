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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ws.common.logger.SalvarLog;
import ws.erh.cadastro.processo.dto.ProcessoModeloRequest;
import ws.erh.cadastro.processo.dto.ProcessoModeloResponse;
import ws.erh.cadastro.processo.service.ProcessoModeloServiceInterface;
import ws.erh.core.enums.processo.CategoriaProcesso;
import ws.erh.core.enums.processo.TipoCampo;
import ws.erh.core.enums.processo.TipoResponsavel;
import ws.erh.core.exception.EntityNotFoundException;
import ws.erh.core.tenant.FiltroTenant;
import ws.erh.model.cadastro.processo.ProcessoCampoModelo;
import ws.erh.model.cadastro.processo.ProcessoDocumentoModelo;
import ws.erh.model.cadastro.processo.ProcessoEtapaModelo;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "https://wsolucoes.com", "https://www.wsolucoes.com"})
@RestController
@RequestMapping("/api/v1/")
public class ProcessoModeloController {

    @Autowired
    private ProcessoModeloServiceInterface processoModeloService;

    @PersistenceContext
    private EntityManager entityManager;

    @SalvarLog(acao = "Criou modelo de processo")
    @PostMapping("processo/modelos")
    public ProcessoModeloResponse createModelo(@Valid @RequestBody ProcessoModeloRequest request) {
        try {
            ProcessoModelo modelo = convertRequestToEntity(request);
            return new ProcessoModeloResponse(processoModeloService.saveProcessoModelo(modelo));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @FiltroTenant
    @GetMapping("processo/modelos")
    public Page<ProcessoModeloResponse> getAllModelos(
            @PageableDefault(size = 20) @SortDefault(sort = "nome", direction = Direction.ASC) Pageable pageable) {
        enableFilters();
        Page<ProcessoModelo> page = processoModeloService.getAll(pageable);
        List<ProcessoModeloResponse> content = page.getContent().stream()
                .map(ProcessoModeloResponse::new)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @FiltroTenant
    @GetMapping("processo/modelos/{id}")
    public ProcessoModeloResponse getModeloById(@PathVariable Long id) {
        enableFilters();
        try {
            return new ProcessoModeloResponse(processoModeloService.findById(id));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @FiltroTenant
    @GetMapping("processo/modelos/codigo/{codigo}")
    public ProcessoModeloResponse getModeloByCodigo(@PathVariable String codigo) {
        enableFilters();
        try {
            return new ProcessoModeloResponse(processoModeloService.findByCodigo(codigo));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @FiltroTenant
    @GetMapping("processo/modelos/ativos")
    public List<ProcessoModeloResponse> getModelosAtivos() {
        enableFilters();
        return processoModeloService.findAtivos().stream()
                .map(ProcessoModeloResponse::new)
                .collect(Collectors.toList());
    }

    @FiltroTenant
    @GetMapping("processo/modelos/categoria/{categoria}")
    public List<ProcessoModeloResponse> getModelosByCategoria(@PathVariable String categoria) {
        enableFilters();
        try {
            CategoriaProcesso cat = CategoriaProcesso.valueOf(categoria);
            return processoModeloService.findByCategoria(cat).stream()
                    .map(ProcessoModeloResponse::new)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria inválida: " + categoria);
        }
    }

    @SalvarLog(acao = "Atualizou modelo de processo")
    @PutMapping("processo/modelos/{id}")
    public ProcessoModeloResponse updateModelo(@PathVariable Long id,
                                               @Valid @RequestBody ProcessoModeloRequest request) {
        try {
            ProcessoModelo entity = processoModeloService.findById(id);
            updateEntityFromRequest(entity, request);
            return new ProcessoModeloResponse(processoModeloService.updateProcessoModelo(entity));
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @SalvarLog(acao = "Excluiu modelo de processo")
    @DeleteMapping("processo/modelos/{id}")
    public String deleteModelo(@PathVariable Long id) {
        try {
            processoModeloService.deleteProcessoModelo(id);
            return "Modelo de processo excluído com sucesso";
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private ProcessoModelo convertRequestToEntity(ProcessoModeloRequest request) {
        ProcessoModelo modelo = new ProcessoModelo();
        modelo.setCodigo(request.getCodigo());
        applyBasicFields(modelo, request);
        List<ProcessoEtapaModelo> etapas = buildEtapas(request.getEtapas(), modelo);
        modelo.setEtapas(etapas);
        modelo.setDocumentosExigidos(buildDocumentos(request.getDocumentosExigidos(), modelo, etapas));
        modelo.setCamposAdicionais(buildCampos(request.getCamposAdicionais(), modelo, etapas));
        return modelo;
    }

    private void updateEntityFromRequest(ProcessoModelo entity, ProcessoModeloRequest request) {
        applyBasicFields(entity, request);
        entity.getEtapas().clear();
        List<ProcessoEtapaModelo> etapas = buildEtapas(request.getEtapas(), entity);
        entity.getEtapas().addAll(etapas);

        entity.getDocumentosExigidos().clear();
        entity.getDocumentosExigidos().addAll(buildDocumentos(request.getDocumentosExigidos(), entity, etapas));

        entity.getCamposAdicionais().clear();
        entity.getCamposAdicionais().addAll(buildCampos(request.getCamposAdicionais(), entity, etapas));
    }

    private void applyBasicFields(ProcessoModelo modelo, ProcessoModeloRequest request) {
        modelo.setNome(request.getNome());
        modelo.setDescricao(request.getDescricao());
        modelo.setInstrucoes(request.getInstrucoes());
        if (request.getCategoria() != null) {
            modelo.setCategoria(CategoriaProcesso.valueOf(request.getCategoria()));
        }
        modelo.setIcone(request.getIcone());
        modelo.setCor(request.getCor());
        modelo.setPrazoAtendimentoDias(request.getPrazoAtendimentoDias());
        modelo.setRequerAprovacaoChefia(Boolean.TRUE.equals(request.getRequerAprovacaoChefia()));
        modelo.setGeraAcaoAutomatica(Boolean.TRUE.equals(request.getGeraAcaoAutomatica()));
        modelo.setAtivo(request.getAtivo() == null || request.getAtivo());
        modelo.setVisivelPortal(request.getVisivelPortal() == null || request.getVisivelPortal());
        modelo.setOrdemExibicao(request.getOrdemExibicao());
    }

    private List<ProcessoEtapaModelo> buildEtapas(List<ProcessoModeloRequest.EtapaModeloRequest> requests,
                                                  ProcessoModelo modelo) {
        List<ProcessoEtapaModelo> etapas = new ArrayList<>();
        if (requests == null) {
            return etapas;
        }
        for (ProcessoModeloRequest.EtapaModeloRequest request : requests) {
            ProcessoEtapaModelo etapa = new ProcessoEtapaModelo();
            etapa.setId(request.getId());
            etapa.setProcessoModelo(modelo);
            etapa.setNome(request.getNome());
            etapa.setDescricao(request.getDescricao());
            etapa.setOrdem(request.getOrdem());
            etapa.setTipoResponsavel(request.getTipoResponsavel() != null
                    ? TipoResponsavel.valueOf(request.getTipoResponsavel())
                    : TipoResponsavel.RH);
            etapa.setAcaoAutomatica(request.getAcaoAutomatica());
            etapa.setPrazoDias(request.getPrazoDias());
            etapas.add(etapa);
        }
        return etapas;
    }

    private List<ProcessoDocumentoModelo> buildDocumentos(List<ProcessoModeloRequest.DocumentoModeloRequest> requests,
                                                          ProcessoModelo modelo,
                                                          List<ProcessoEtapaModelo> etapas) {
        List<ProcessoDocumentoModelo> documentos = new ArrayList<>();
        if (requests == null) {
            return documentos;
        }
        for (ProcessoModeloRequest.DocumentoModeloRequest request : requests) {
            ProcessoDocumentoModelo doc = new ProcessoDocumentoModelo();
            doc.setId(request.getId());
            doc.setProcessoModelo(modelo);
            doc.setNome(request.getNome());
            doc.setDescricao(request.getDescricao());
            doc.setObrigatorio(request.getObrigatorio() == null || request.getObrigatorio());
            doc.setTiposPermitidos(request.getTiposPermitidos());
            doc.setTamanhoMaximoMb(request.getTamanhoMaximoMb());
            doc.setModeloUrl(request.getModeloUrl());
            doc.setOrdem(request.getOrdem());
            doc.setEtapaModelo(resolveEtapa(etapas, request.getEtapaModeloId(), request.getEtapaOrdem()));
            documentos.add(doc);
        }
        return documentos;
    }

    private List<ProcessoCampoModelo> buildCampos(List<ProcessoModeloRequest.CampoModeloRequest> requests,
                                                  ProcessoModelo modelo,
                                                  List<ProcessoEtapaModelo> etapas) {
        List<ProcessoCampoModelo> campos = new ArrayList<>();
        if (requests == null) {
            return campos;
        }
        for (ProcessoModeloRequest.CampoModeloRequest request : requests) {
            ProcessoCampoModelo campo = new ProcessoCampoModelo();
            campo.setId(request.getId());
            campo.setProcessoModelo(modelo);
            campo.setNomeCampo(request.getNomeCampo());
            campo.setLabel(request.getLabel());
            campo.setTipoCampo(request.getTipoCampo() != null
                    ? TipoCampo.valueOf(request.getTipoCampo())
                    : TipoCampo.TEXT);
            campo.setObrigatorio(request.getObrigatorio() == null || request.getObrigatorio());
            campo.setOpcoesSelect(request.getOpcoesSelect());
            campo.setPlaceholder(request.getPlaceholder());
            campo.setAjuda(request.getAjuda());
            campo.setOrdem(request.getOrdem());
            campo.setEtapaModelo(resolveEtapa(etapas, request.getEtapaModeloId(), request.getEtapaOrdem()));
            campos.add(campo);
        }
        return campos;
    }

    private ProcessoEtapaModelo resolveEtapa(List<ProcessoEtapaModelo> etapas, Long etapaModeloId, Integer etapaOrdem) {
        if (etapas == null || etapas.isEmpty()) {
            return null;
        }
        if (etapaModeloId != null) {
            for (ProcessoEtapaModelo etapa : etapas) {
                if (etapaModeloId.equals(etapa.getId())) {
                    return etapa;
                }
            }
        }
        if (etapaOrdem != null) {
            for (ProcessoEtapaModelo etapa : etapas) {
                if (etapaOrdem.equals(etapa.getOrdem())) {
                    return etapa;
                }
            }
        }
        return null;
    }

    private void enableFilters() {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("excluidoFilter").setParameter("excluido", false);
    }
}
