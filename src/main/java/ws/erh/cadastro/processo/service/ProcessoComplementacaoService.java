package ws.erh.cadastro.processo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ws.erh.cadastro.portal.service.PortalNotificacaoServiceInterface;
import ws.erh.cadastro.processo.dto.ProcessoComplementacaoRequest;
import ws.erh.cadastro.processo.dto.ProcessoComplementacaoRespostaRequest;
import ws.erh.cadastro.processo.repository.ProcessoCampoModeloRepository;
import ws.erh.cadastro.processo.repository.ProcessoComplementacaoRepository;
import ws.erh.cadastro.processo.repository.ProcessoDocumentoModeloRepository;
import ws.erh.cadastro.processo.repository.ProcessoHistoricoRepository;
import ws.erh.cadastro.processo.repository.ProcessoRepository;
import ws.erh.core.base.AbstractTenantService;
import ws.erh.core.enums.portal.TipoNotificacao;
import ws.erh.core.enums.processo.AcaoProcesso;
import ws.erh.core.enums.processo.Prioridade;
import ws.erh.core.enums.processo.StatusComplementacaoProcesso;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.core.enums.processo.TipoAutor;
import ws.erh.core.enums.processo.TipoItemComplementacaoProcesso;
import ws.erh.core.exception.EntityNotFoundException;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoCampoModelo;
import ws.erh.model.cadastro.processo.ProcessoComplementacao;
import ws.erh.model.cadastro.processo.ProcessoComplementacaoItem;
import ws.erh.model.cadastro.processo.ProcessoDocumento;
import ws.erh.model.cadastro.processo.ProcessoDocumentoModelo;
import ws.erh.model.cadastro.processo.ProcessoHistorico;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ProcessoComplementacaoService extends AbstractTenantService {

    private final ProcessoServiceInterface processoService;
    private final ProcessoRepository processoRepository;
    private final ProcessoComplementacaoRepository complementacaoRepository;
    private final ProcessoDocumentoModeloRepository documentoModeloRepository;
    private final ProcessoCampoModeloRepository campoModeloRepository;
    private final ProcessoDocumentoServiceInterface documentoService;
    private final ProcessoHistoricoRepository historicoRepository;
    private final ProcessoWorkflowService workflowService;
    private final PortalNotificacaoServiceInterface notificacaoService;

    public ProcessoComplementacaoService(ProcessoServiceInterface processoService,
                                        ProcessoRepository processoRepository,
                                        ProcessoComplementacaoRepository complementacaoRepository,
                                        ProcessoDocumentoModeloRepository documentoModeloRepository,
                                        ProcessoCampoModeloRepository campoModeloRepository,
                                        ProcessoDocumentoServiceInterface documentoService,
                                        ProcessoHistoricoRepository historicoRepository,
                                        ProcessoWorkflowService workflowService,
                                        PortalNotificacaoServiceInterface notificacaoService) {
        this.processoService = processoService;
        this.processoRepository = processoRepository;
        this.complementacaoRepository = complementacaoRepository;
        this.documentoModeloRepository = documentoModeloRepository;
        this.campoModeloRepository = campoModeloRepository;
        this.documentoService = documentoService;
        this.historicoRepository = historicoRepository;
        this.workflowService = workflowService;
        this.notificacaoService = notificacaoService;
    }

    public Processo solicitarComplementacao(Long processoId, ProcessoComplementacaoRequest request, String usuario) {
        Processo processo = processoService.findById(processoId);
        cancelarComplementacaoAberta(processo);

        SituacaoProcesso situacaoAnterior = processo.getSituacao();
        Integer etapaAnterior = processo.getEtapaAtual();
        workflowService.solicitarComplementacao(processo);
        processo.setDataUltimaAtualizacao(LocalDateTime.now());
        configurarDadosTenant(processo);
        Processo savedProcesso = processoRepository.save(processo);

        ProcessoComplementacao complementacao = new ProcessoComplementacao();
        complementacao.setProcesso(savedProcesso);
        complementacao.setEtapaReferencia(etapaAnterior != null ? etapaAnterior : savedProcesso.getEtapaAtual());
        complementacao.setEtapaNomeSnapshot(workflowService.resolveEtapaAtualNome(savedProcesso));
        complementacao.setSituacaoRetorno(situacaoAnterior != null ? situacaoAnterior : SituacaoProcesso.ABERTO);
        complementacao.setStatus(StatusComplementacaoProcesso.ABERTA);
        complementacao.setPrazoLimite(parsePrazo(request.getPrazoLimite(), savedProcesso.getPrazoLimite()));
        complementacao.setMotivoConsolidado(resolveMotivoConsolidado(request));
        complementacao.setSolicitadoPor(usuario != null ? usuario : "RH");
        complementacao.setTipoSolicitante(TipoAutor.RH);
        complementacao.setDataSolicitacao(LocalDateTime.now());

        List<ProcessoComplementacaoItem> itens = buildItens(complementacao, request);
        complementacao.setItens(itens);
        configurarDadosTenant(complementacao);
        ProcessoComplementacao savedComplementacao = complementacaoRepository.save(complementacao);

        registrarHistorico(savedProcesso,
                AcaoProcesso.DOCUMENTACAO_SOLICITADA,
                situacaoAnterior != null ? situacaoAnterior.name() : null,
                SituacaoProcesso.PENDENTE_DOCUMENTACAO.name(),
                etapaAnterior,
                savedProcesso.getEtapaAtual(),
                usuario != null ? usuario : "RH",
                TipoAutor.RH,
                savedComplementacao.getMotivoConsolidado());

        notificarServidor(savedProcesso, savedComplementacao);
        return savedProcesso;
    }

    public Processo responderComplementacao(Long processoId,
                                            Long servidorId,
                                            Long complementacaoId,
                                            ProcessoComplementacaoRespostaRequest request,
                                            List<MultipartFile> arquivos,
                                            String usuario) {
        Processo processo = processoService.findByIdAndServidorId(processoId, servidorId);
        ProcessoComplementacao complementacao = findComplementacao(complementacaoId);
        if (!complementacao.getProcesso().getId().equals(processo.getId())) {
            throw new IllegalStateException("A complementação não pertence ao processo informado.");
        }
        if (complementacao.getStatus() != StatusComplementacaoProcesso.ABERTA) {
            throw new IllegalStateException("A complementação informada não está aberta para resposta.");
        }

        if (request.getDadosFormulario() != null) {
            processo.setDadosFormulario(request.getDadosFormulario());
        }
        if (request.getObservacaoServidor() != null) {
            processo.setObservacaoServidor(request.getObservacaoServidor());
        }
        if (request.getPrioridade() != null && !request.getPrioridade().isBlank()) {
            processo.setPrioridade(Prioridade.valueOf(request.getPrioridade()));
        }

        List<Long> uploadedIds = new ArrayList<>();
        try {
            vincularAnexosResposta(complementacao, processo, request, arquivos, usuario, uploadedIds);

            SituacaoProcesso situacaoAnterior = processo.getSituacao();
            processo.setSituacao(complementacao.getSituacaoRetorno());
            processo.setDataUltimaAtualizacao(LocalDateTime.now());
            configurarDadosTenant(processo);
            Processo savedProcesso = processoRepository.save(processo);

            complementacao.setStatus(StatusComplementacaoProcesso.RESPONDIDA);
            complementacao.setRespondidoPor(usuario != null ? usuario : "Servidor");
            complementacao.setDataResposta(LocalDateTime.now());
            configurarDadosTenant(complementacao);
            complementacaoRepository.save(complementacao);

            registrarHistorico(savedProcesso,
                    AcaoProcesso.REABERTO,
                    situacaoAnterior != null ? situacaoAnterior.name() : null,
                    complementacao.getSituacaoRetorno() != null ? complementacao.getSituacaoRetorno().name() : null,
                    savedProcesso.getEtapaAtual(),
                    savedProcesso.getEtapaAtual(),
                    usuario != null ? usuario : "Servidor",
                    TipoAutor.SERVIDOR,
                    "Servidor respondeu à complementação solicitada.");

            return savedProcesso;
        } catch (Exception e) {
            rollbackUpload(uploadedIds);
            throw e;
        }
    }

    public ProcessoComplementacao encerrarComplementacao(Long complementacaoId, String usuario) {
        ProcessoComplementacao complementacao = findComplementacao(complementacaoId);
        complementacao.setStatus(StatusComplementacaoProcesso.ENCERRADA);
        complementacao.setDataEncerramento(LocalDateTime.now());
        if (usuario != null && !usuario.isBlank()) {
            complementacao.setRespondidoPor(usuario);
        }
        configurarDadosTenant(complementacao);
        return complementacaoRepository.save(complementacao);
    }

    private void cancelarComplementacaoAberta(Processo processo) {
        complementacaoRepository.findFirstByProcessoIdAndStatus(processo.getId(), StatusComplementacaoProcesso.ABERTA)
                .ifPresent(complementacao -> {
                    complementacao.setStatus(StatusComplementacaoProcesso.CANCELADA);
                    complementacao.setDataEncerramento(LocalDateTime.now());
                    configurarDadosTenant(complementacao);
                    complementacaoRepository.save(complementacao);
                });
    }

    private List<ProcessoComplementacaoItem> buildItens(ProcessoComplementacao complementacao,
                                                        ProcessoComplementacaoRequest request) {
        List<ProcessoComplementacaoItem> itens = new ArrayList<>();
        if (request.getItens() == null || request.getItens().isEmpty()) {
            ProcessoComplementacaoItem item = new ProcessoComplementacaoItem();
            item.setComplementacao(complementacao);
            item.setTipoItem(TipoItemComplementacaoProcesso.DOCUMENTO_LIVRE);
            item.setLabel("Documentação complementar");
            item.setObrigatorio(true);
            item.setMotivo(resolveMotivoConsolidado(request));
            item.setOrdem(1);
            configurarDadosTenant(item);
            itens.add(item);
            return itens;
        }

        int ordem = 1;
        for (ProcessoComplementacaoRequest.ItemRequest itemRequest : request.getItens()) {
            ProcessoComplementacaoItem item = new ProcessoComplementacaoItem();
            item.setComplementacao(complementacao);
            item.setTipoItem(resolveTipoItem(itemRequest.getTipoItem()));
            item.setLabel(itemRequest.getLabel() != null && !itemRequest.getLabel().isBlank()
                    ? itemRequest.getLabel()
                    : resolveLabelItem(itemRequest));
            item.setObrigatorio(itemRequest.getObrigatorio() == null || itemRequest.getObrigatorio());
            item.setMotivo(itemRequest.getMotivo());
            item.setOrdem(itemRequest.getOrdem() != null ? itemRequest.getOrdem() : ordem++);

            if (itemRequest.getDocumentoModeloId() != null) {
                ProcessoDocumentoModelo documentoModelo = documentoModeloRepository.findById(itemRequest.getDocumentoModeloId())
                        .orElseThrow(() -> new IllegalStateException("Documento do modelo não encontrado: " + itemRequest.getDocumentoModeloId()));
                item.setDocumentoModelo(documentoModelo);
            }
            if (itemRequest.getCampoModeloId() != null) {
                ProcessoCampoModelo campoModelo = campoModeloRepository.findById(itemRequest.getCampoModeloId())
                        .orElseThrow(() -> new IllegalStateException("Campo do modelo não encontrado: " + itemRequest.getCampoModeloId()));
                item.setCampoModelo(campoModelo);
            }
            configurarDadosTenant(item);
            itens.add(item);
        }
        return itens;
    }

    private void vincularAnexosResposta(ProcessoComplementacao complementacao,
                                        Processo processo,
                                        ProcessoComplementacaoRespostaRequest request,
                                        List<MultipartFile> arquivos,
                                        String usuario,
                                        List<Long> uploadedIds) {
        if (arquivos == null || arquivos.isEmpty()) {
            return;
        }
        List<ProcessoComplementacaoRespostaRequest.AnexoRespostaRequest> anexos = request.getAnexos() != null
                ? request.getAnexos()
                : List.of();
        if (!anexos.isEmpty() && anexos.size() != arquivos.size()) {
            throw new IllegalStateException("A quantidade de anexos informados não corresponde aos arquivos enviados.");
        }

        for (int i = 0; i < arquivos.size(); i++) {
            MultipartFile arquivo = arquivos.get(i);
            ProcessoComplementacaoRespostaRequest.AnexoRespostaRequest anexo = anexos.isEmpty()
                    ? null
                    : anexos.get(i);

            ProcessoComplementacaoItem item = null;
            Long documentoModeloId = null;
            if (anexo != null && anexo.getItemId() != null) {
                item = complementacao.getItens().stream()
                        .filter(existing -> anexo.getItemId().equals(existing.getId()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Item de complementação não encontrado: " + anexo.getItemId()));
                if (item.getDocumentoModelo() != null) {
                    documentoModeloId = item.getDocumentoModelo().getId();
                }
            }
            if (documentoModeloId == null && anexo != null) {
                documentoModeloId = anexo.getDocumentoModeloId();
            }

            ProcessoDocumento documento = documentoService.uploadDocumento(
                    processo,
                    arquivo,
                    documentoModeloId,
                    usuario != null ? usuario : "Servidor");
            uploadedIds.add(documento.getId());
            if (item != null) {
                item.setDocumentoRespondido(documento);
                configurarDadosTenant(item);
            }
        }
    }

    private String resolveMotivoConsolidado(ProcessoComplementacaoRequest request) {
        if (request.getMotivoConsolidado() != null && !request.getMotivoConsolidado().isBlank()) {
            return request.getMotivoConsolidado();
        }
        if (request.getItens() == null || request.getItens().isEmpty()) {
            return "Documentação complementar solicitada pelo RH.";
        }
        return request.getItens().stream()
                .map(item -> item.getMotivo() != null && !item.getMotivo().isBlank()
                        ? item.getMotivo()
                        : item.getLabel())
                .filter(text -> text != null && !text.isBlank())
                .distinct()
                .reduce((left, right) -> left + " | " + right)
                .orElse("Documentação complementar solicitada pelo RH.");
    }

    private String resolveLabelItem(ProcessoComplementacaoRequest.ItemRequest request) {
        if (request.getDocumentoModeloId() != null) {
            return documentoModeloRepository.findById(request.getDocumentoModeloId())
                    .map(ProcessoDocumentoModelo::getNome)
                    .orElse("Documento complementar");
        }
        if (request.getCampoModeloId() != null) {
            return campoModeloRepository.findById(request.getCampoModeloId())
                    .map(ProcessoCampoModelo::getLabel)
                    .orElse("Campo complementar");
        }
        return "Item complementar";
    }

    private TipoItemComplementacaoProcesso resolveTipoItem(String tipoItem) {
        if (tipoItem == null || tipoItem.isBlank()) {
            return TipoItemComplementacaoProcesso.DOCUMENTO_LIVRE;
        }
        return TipoItemComplementacaoProcesso.valueOf(tipoItem);
    }

    private LocalDate parsePrazo(String prazo, LocalDate fallback) {
        if (prazo == null || prazo.isBlank()) {
            return fallback;
        }
        return LocalDate.parse(prazo);
    }

    private ProcessoComplementacao findComplementacao(Long complementacaoId) {
        return complementacaoRepository.findById(complementacaoId)
                .orElseThrow(() -> new EntityNotFoundException("Complementação não encontrada: " + complementacaoId));
    }

    private void rollbackUpload(List<Long> uploadedIds) {
        for (Long documentoId : uploadedIds) {
            try {
                documentoService.deleteDocumentoWithStorage(documentoId);
            } catch (Exception ignored) {
            }
        }
    }

    private void registrarHistorico(Processo processo,
                                    AcaoProcesso acao,
                                    String situacaoAnterior,
                                    String situacaoNova,
                                    Integer etapaAnterior,
                                    Integer etapaNova,
                                    String usuario,
                                    TipoAutor tipoAutor,
                                    String descricao) {
        ProcessoHistorico historico = new ProcessoHistorico();
        historico.setProcesso(processo);
        historico.setDataHora(LocalDateTime.now());
        historico.setAcao(acao);
        historico.setSituacaoAnterior(situacaoAnterior);
        historico.setSituacaoNova(situacaoNova);
        historico.setEtapaAnterior(etapaAnterior);
        historico.setEtapaNova(etapaNova);
        historico.setUsuario(usuario);
        historico.setTipoUsuario(tipoAutor);
        historico.setDescricao(descricao);
        historico.setUnidadeGestoraId(processo.getUnidadeGestoraId());
        historicoRepository.save(historico);
    }

    private void notificarServidor(Processo processo, ProcessoComplementacao complementacao) {
        if (processo.getServidorId() == null) {
            return;
        }
        notificacaoService.criarNotificacao(
                processo.getServidorId(),
                "Complementação solicitada",
                "O processo " + processo.getProtocolo() + " está aguardando documentos ou correções adicionais.",
                TipoNotificacao.SOLICITACAO,
                "/portal/processos",
                "/e-RH/portal-servidor/processos/" + processo.getId(),
                processo.getId(),
                complementacao.getEtapaReferencia() != null ? "ETAPA_" + complementacao.getEtapaReferencia() : "ABERTURA",
                "COMPLEMENTAR");
    }
}
