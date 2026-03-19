package ws.erh.cadastro.processo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ws.erh.cadastro.processo.dto.ProcessoComplementacaoRespostaRequest;
import ws.erh.cadastro.processo.dto.ProcessoMensagemRequest;
import ws.erh.cadastro.processo.dto.ProcessoPortalComplementoRequest;
import ws.erh.cadastro.processo.dto.ProcessoPortalSubmissaoRequest;
import ws.erh.cadastro.processo.repository.ProcessoComplementacaoRepository;
import ws.erh.cadastro.processo.repository.ProcessoHistoricoRepository;
import ws.erh.cadastro.processo.repository.ProcessoRepository;
import ws.erh.core.base.AbstractTenantService;
import ws.erh.core.enums.processo.AcaoProcesso;
import ws.erh.core.enums.processo.Prioridade;
import ws.erh.core.enums.processo.StatusComplementacaoProcesso;
import ws.erh.core.enums.processo.TipoAutor;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoComplementacao;
import ws.erh.model.cadastro.processo.ProcessoDocumento;
import ws.erh.model.cadastro.processo.ProcessoHistorico;
import ws.erh.model.cadastro.processo.ProcessoMensagem;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ProcessoPortalService extends AbstractTenantService {

    private final ProcessoServiceInterface processoService;
    private final ProcessoRepository processoRepository;
    private final ProcessoModeloServiceInterface processoModeloService;
    private final ProcessoDocumentoServiceInterface documentoService;
    private final ProcessoMensagemServiceInterface mensagemService;
    private final ProcessoWorkflowService workflowService;
    private final ProcessoValidacaoService validacaoService;
    private final ProcessoHistoricoRepository historicoRepository;
    private final ProcessoComplementacaoRepository complementacaoRepository;
    private final ProcessoComplementacaoService complementacaoService;

    public ProcessoPortalService(ProcessoServiceInterface processoService,
                                 ProcessoRepository processoRepository,
                                 ProcessoModeloServiceInterface processoModeloService,
                                 ProcessoDocumentoServiceInterface documentoService,
                                 ProcessoMensagemServiceInterface mensagemService,
                                 ProcessoWorkflowService workflowService,
                                 ProcessoValidacaoService validacaoService,
                                 ProcessoHistoricoRepository historicoRepository,
                                 ProcessoComplementacaoRepository complementacaoRepository,
                                 ProcessoComplementacaoService complementacaoService) {
        this.processoService = processoService;
        this.processoRepository = processoRepository;
        this.processoModeloService = processoModeloService;
        this.documentoService = documentoService;
        this.mensagemService = mensagemService;
        this.workflowService = workflowService;
        this.validacaoService = validacaoService;
        this.historicoRepository = historicoRepository;
        this.complementacaoRepository = complementacaoRepository;
        this.complementacaoService = complementacaoService;
    }

    public org.springframework.data.domain.Page<Processo> getMeusProcessos(Long servidorId,
                                                                           org.springframework.data.domain.Pageable pageable) {
        return processoService.findByServidorId(servidorId, pageable);
    }

    public Processo getMeuProcesso(Long processoId, Long servidorId) {
        return processoService.findByIdAndServidorId(processoId, servidorId);
    }

    public Processo submeter(Long servidorId,
                             ProcessoPortalSubmissaoRequest request,
                             List<MultipartFile> arquivos,
                             List<Long> documentoModeloIds,
                             String enviadoPor) {
        ProcessoModelo modelo = processoModeloService.findById(request.getProcessoModeloId());
        validacaoService.validarSubmissao(modelo, request.getDadosFormulario(), documentoModeloIds);

        Processo processo = new Processo();
        processo.setProcessoModelo(modelo);
        processo.setServidorId(servidorId);
        processo.setVinculoFuncionalId(request.getVinculoFuncionalId());
        processo.setDadosFormulario(request.getDadosFormulario());
        processo.setObservacaoServidor(request.getObservacaoServidor());
        processo.setPrioridade(parsePrioridade(request.getPrioridade()));

        Processo saved = processoService.abrirProcesso(processo);
        List<Long> uploadedIds = new ArrayList<>();
        try {
            uploadDocumentos(saved, arquivos, documentoModeloIds, enviadoPor, uploadedIds);
            return processoService.findById(saved.getId());
        } catch (Exception e) {
            rollbackUpload(uploadedIds);
            processoRepository.delete(saved);
            throw e;
        }
    }

    public Processo complementar(Long processoId,
                                 Long servidorId,
                                 ProcessoPortalComplementoRequest request,
                                 List<MultipartFile> arquivos,
                                 List<Long> documentoModeloIds,
                                 String enviadoPor) {
        Processo processo = processoService.findByIdAndServidorId(processoId, servidorId);
        ProcessoComplementacao complementacao = complementacaoRepository
                .findFirstByProcessoIdAndStatus(processoId, StatusComplementacaoProcesso.ABERTA)
                .orElse(null);

        if (complementacao != null) {
            ProcessoComplementacaoRespostaRequest resposta = new ProcessoComplementacaoRespostaRequest();
            resposta.setDadosFormulario(request.getDadosFormulario());
            resposta.setObservacaoServidor(request.getObservacaoServidor());
            resposta.setPrioridade(request.getPrioridade());
            if (documentoModeloIds != null) {
                for (Long documentoModeloId : documentoModeloIds) {
                    ProcessoComplementacaoRespostaRequest.AnexoRespostaRequest anexo = new ProcessoComplementacaoRespostaRequest.AnexoRespostaRequest();
                    anexo.setDocumentoModeloId(documentoModeloId);
                    resposta.getAnexos().add(anexo);
                }
            }
            return complementacaoService.responderComplementacao(
                    processoId,
                    servidorId,
                    complementacao.getId(),
                    resposta,
                    arquivos,
                    enviadoPor);
        }

        if (!workflowService.podeComplementar(processo)) {
            throw new IllegalStateException("O processo não está disponível para complementação.");
        }

        if (request.getDadosFormulario() != null) {
            processo.setDadosFormulario(request.getDadosFormulario());
        }
        if (request.getObservacaoServidor() != null) {
            processo.setObservacaoServidor(request.getObservacaoServidor());
        }
        if (request.getPrioridade() != null) {
            processo.setPrioridade(parsePrioridade(request.getPrioridade()));
        }

        List<Long> uploadedIds = new ArrayList<>();
        try {
            uploadDocumentos(processo, arquivos, documentoModeloIds, enviadoPor, uploadedIds);

            Processo atualizado = processoService.findById(processoId);
            atualizado.setDadosFormulario(processo.getDadosFormulario());
            atualizado.setObservacaoServidor(processo.getObservacaoServidor());
            atualizado.setPrioridade(processo.getPrioridade());

            String situacaoAnterior = atualizado.getSituacao().name();
            Integer etapaAnterior = atualizado.getEtapaAtual();
            workflowService.retomarAposComplementacao(atualizado);
            atualizado.setDataUltimaAtualizacao(LocalDateTime.now());
            configurarDadosTenant(atualizado);
            Processo saved = processoRepository.save(atualizado);

            registrarHistorico(saved, AcaoProcesso.REABERTO, situacaoAnterior, saved.getSituacao().name(),
                    etapaAnterior, saved.getEtapaAtual(), enviadoPor, TipoAutor.SERVIDOR,
                    "Servidor complementou o processo e reenviou a documentação.");
            return saved;
        } catch (Exception e) {
            rollbackUpload(uploadedIds);
            throw e;
        }
    }

    public Processo responderComplementacao(Long processoId,
                                            Long servidorId,
                                            Long complementacaoId,
                                            ProcessoComplementacaoRespostaRequest request,
                                            List<MultipartFile> arquivos,
                                            String enviadoPor) {
        validarOwnership(processoId, servidorId);
        return complementacaoService.responderComplementacao(
                processoId,
                servidorId,
                complementacaoId,
                request,
                arquivos,
                enviadoPor);
    }

    public Processo cancelar(Long processoId, Long servidorId, String justificativa, String usuario) {
        Processo processo = processoService.findByIdAndServidorId(processoId, servidorId);
        return processoService.cancelar(processo.getId(), justificativa, usuario);
    }

    public List<ProcessoDocumento> getDocumentos(Long processoId, Long servidorId) {
        Processo processo = processoService.findByIdAndServidorId(processoId, servidorId);
        return documentoService.findByProcessoId(processo.getId());
    }

    public ProcessoDocumento getDocumento(Long documentoId, Long servidorId) {
        ProcessoDocumento documento = documentoService.findById(documentoId);
        validarOwnership(documento.getProcesso().getId(), servidorId);
        return documento;
    }

    public byte[] downloadDocumento(Long documentoId, Long servidorId) {
        ProcessoDocumento documento = documentoService.findById(documentoId);
        validarOwnership(documento.getProcesso().getId(), servidorId);
        return documentoService.downloadDocumento(documentoId);
    }

    public List<ProcessoMensagem> getMensagens(Long processoId, Long servidorId) {
        validarOwnership(processoId, servidorId);
        return mensagemService.findByProcessoId(processoId);
    }

    public ProcessoMensagem enviarMensagem(Long processoId, Long servidorId, ProcessoMensagemRequest request) {
        Processo processo = processoService.findByIdAndServidorId(processoId, servidorId);

        ProcessoMensagem mensagem = new ProcessoMensagem();
        mensagem.setProcesso(processo);
        mensagem.setAutor(request.getAutor() != null ? request.getAutor() : "Servidor");
        mensagem.setTipoAutor(TipoAutor.SERVIDOR);
        mensagem.setMensagem(request.getMensagem());
        ProcessoMensagem saved = mensagemService.enviarMensagem(mensagem);

        registrarHistorico(processo, AcaoProcesso.MENSAGEM_SERVIDOR, processo.getSituacao().name(), processo.getSituacao().name(),
                processo.getEtapaAtual(), processo.getEtapaAtual(), mensagem.getAutor(), TipoAutor.SERVIDOR,
                "Servidor enviou uma mensagem no processo.");

        return saved;
    }

    public void marcarMensagensComoLidas(Long processoId, Long servidorId) {
        validarOwnership(processoId, servidorId);
        mensagemService.marcarComoLidas(processoId);
    }

    public List<ProcessoHistorico> getHistorico(Long processoId, Long servidorId) {
        Processo processo = processoService.findByIdAndServidorId(processoId, servidorId);
        return processo.getHistorico();
    }

    private void uploadDocumentos(Processo processo,
                                  List<MultipartFile> arquivos,
                                  List<Long> documentoModeloIds,
                                  String enviadoPor,
                                  List<Long> uploadedIds) {
        if (arquivos == null || arquivos.isEmpty()) {
            return;
        }
        if (documentoModeloIds == null || documentoModeloIds.size() != arquivos.size()) {
            throw new IllegalStateException("Os documentos enviados devem corresponder aos arquivos informados.");
        }

        for (int i = 0; i < arquivos.size(); i++) {
            MultipartFile arquivo = arquivos.get(i);
            Long documentoModeloId = documentoModeloIds.get(i);
            ProcessoDocumento documento = documentoService.uploadDocumento(processo, arquivo, documentoModeloId, enviadoPor);
            uploadedIds.add(documento.getId());
        }
    }

    private void rollbackUpload(List<Long> uploadedIds) {
        for (Long documentoId : uploadedIds) {
            try {
                documentoService.deleteDocumentoWithStorage(documentoId);
            } catch (Exception ignored) {
            }
        }
    }

    private void validarOwnership(Long processoId, Long servidorId) {
        processoService.findByIdAndServidorId(processoId, servidorId);
    }

    private Prioridade parsePrioridade(String prioridade) {
        if (prioridade == null || prioridade.isBlank()) {
            return Prioridade.NORMAL;
        }
        return Prioridade.valueOf(prioridade);
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
}
