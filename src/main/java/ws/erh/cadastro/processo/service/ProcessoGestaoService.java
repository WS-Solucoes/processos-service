package ws.erh.cadastro.processo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ws.erh.cadastro.portal.service.PortalNotificacaoServiceInterface;
import ws.erh.cadastro.processo.dto.ProcessoComplementacaoRequest;
import ws.erh.cadastro.processo.repository.ProcessoHistoricoRepository;
import ws.erh.cadastro.processo.repository.ProcessoRepository;
import ws.erh.core.base.AbstractTenantService;
import ws.erh.core.enums.portal.TipoNotificacao;
import ws.erh.core.enums.processo.AcaoProcesso;
import ws.erh.core.enums.processo.IntegracaoStatusProcesso;
import ws.erh.core.enums.processo.ResultadoProcesso;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.core.enums.processo.TipoAutor;
import ws.erh.core.exception.EntityNotFoundException;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoHistorico;

import java.time.LocalDateTime;

@Service
@Transactional
@Slf4j
public class ProcessoGestaoService extends AbstractTenantService implements ProcessoGestaoServiceInterface {

    private final ProcessoRepository processoRepository;
    private final ProcessoHistoricoRepository historicoRepository;
    private final PortalNotificacaoServiceInterface notificacaoService;
    private final ProcessoWorkflowService workflowService;
    private final ProcessoValidacaoService validacaoService;
    private final ProcessoComplementacaoService complementacaoService;

    public ProcessoGestaoService(ProcessoRepository processoRepository,
                                 ProcessoHistoricoRepository historicoRepository,
                                 PortalNotificacaoServiceInterface notificacaoService,
                                 ProcessoWorkflowService workflowService,
                                 ProcessoValidacaoService validacaoService,
                                 ProcessoComplementacaoService complementacaoService) {
        this.processoRepository = processoRepository;
        this.historicoRepository = historicoRepository;
        this.notificacaoService = notificacaoService;
        this.workflowService = workflowService;
        this.validacaoService = validacaoService;
        this.complementacaoService = complementacaoService;
    }

    @Override
    public Processo atribuir(Long processoId, String atribuidoPara, String departamento, String usuario) {
        log.info("Atribuindo processo ID: {} para {}", processoId, atribuidoPara);
        Processo processo = findProcesso(processoId);
        processo.setAtribuidoPara(atribuidoPara);
        processo.setDepartamentoAtribuido(departamento);
        processo.setDataUltimaAtualizacao(LocalDateTime.now());
        configurarDadosTenant(processo);
        Processo saved = processoRepository.save(processo);
        registrarHistorico(saved, AcaoProcesso.ATRIBUIDO, null, null, null, null,
                usuario, TipoAutor.RH, "Atribuído para: " + atribuidoPara);
        notificarServidor(saved, "Processo atribuído",
                "Seu processo " + saved.getProtocolo() + " foi atribuído para " + atribuidoPara + ".",
                TipoNotificacao.INFO);
        return saved;
    }

    @Override
    public Processo iniciarAnalise(Long processoId, String usuario) {
        log.info("Iniciando análise do processo ID: {}", processoId);
        Processo processo = findProcesso(processoId);
        validarTransicao(processo, SituacaoProcesso.EM_ANALISE);

        String anterior = processo.getSituacao().name();
        Integer etapaAnterior = processo.getEtapaAtual();
        workflowService.iniciarAnalise(processo);
        processo.setDataUltimaAtualizacao(LocalDateTime.now());
        configurarDadosTenant(processo);
        Processo saved = processoRepository.save(processo);
        registrarHistorico(saved, AcaoProcesso.EM_ANALISE, anterior,
                saved.getSituacao().name(), etapaAnterior, saved.getEtapaAtual(),
                usuario, TipoAutor.RH, "Análise iniciada");
        notificarServidor(saved, "Processo em análise",
                "Seu processo " + saved.getProtocolo() + " está sendo analisado.",
                TipoNotificacao.INFO);
        return saved;
    }

    @Override
    public Processo solicitarDocumentacao(Long processoId, String descricao, String usuario) {
        log.info("Solicitando documentação no processo ID: {}", processoId);
        ProcessoComplementacaoRequest request = new ProcessoComplementacaoRequest();
        request.setMotivoConsolidado(descricao);
        ProcessoComplementacaoRequest.ItemRequest item = new ProcessoComplementacaoRequest.ItemRequest();
        item.setTipoItem("DOCUMENTO_LIVRE");
        item.setLabel("Documentação complementar");
        item.setObrigatorio(true);
        item.setMotivo(descricao);
        item.setOrdem(1);
        request.getItens().add(item);
        return complementacaoService.solicitarComplementacao(processoId, request, usuario != null ? usuario : "RH");
    }

    @Override
    public Processo encaminharChefia(Long processoId, String usuario) {
        log.info("Encaminhando processo ID: {} para chefia", processoId);
        Processo processo = findProcesso(processoId);

        String anterior = processo.getSituacao().name();
        Integer etapaAnterior = processo.getEtapaAtual();
        workflowService.encaminharChefia(processo);
        processo.setDataUltimaAtualizacao(LocalDateTime.now());
        configurarDadosTenant(processo);
        Processo saved = processoRepository.save(processo);
        registrarHistorico(saved, AcaoProcesso.ENCAMINHADO_CHEFIA, anterior,
                SituacaoProcesso.AGUARDANDO_CHEFIA.name(), etapaAnterior, saved.getEtapaAtual(),
                usuario, TipoAutor.RH, "Encaminhado para aprovação da chefia");
        notificarServidor(saved, "Processo encaminhado à chefia",
                "Seu processo " + saved.getProtocolo() + " foi encaminhado para aprovação da chefia.",
                TipoNotificacao.INFO);
        return saved;
    }

    @Override
    public Processo deferir(Long processoId, String justificativa, String usuario) {
        log.info("Deferindo processo ID: {}", processoId);
        Processo processo = findProcesso(processoId);
        validarTransicao(processo, SituacaoProcesso.DEFERIDO);
        validacaoService.validarProcessoParaDeferimento(processo);

        String anterior = processo.getSituacao().name();
        Integer etapaAnterior = processo.getEtapaAtual();
        processo.setSituacao(SituacaoProcesso.DEFERIDO);
        processo.setResultado(ResultadoProcesso.DEFERIDO);
        processo.setJustificativaResultado(justificativa);
        processo.setDataConclusao(LocalDateTime.now());
        processo.setDataUltimaAtualizacao(LocalDateTime.now());
        processo.setIntegracaoErro(null);
        processo.setIntegracaoStatus(Boolean.TRUE.equals(processo.getProcessoModelo().getGeraAcaoAutomatica())
                ? IntegracaoStatusProcesso.PENDENTE
                : IntegracaoStatusProcesso.SUCESSO);
        configurarDadosTenant(processo);
        Processo saved = processoRepository.save(processo);
        registrarHistorico(saved, AcaoProcesso.DEFERIDO, anterior,
                SituacaoProcesso.DEFERIDO.name(), etapaAnterior, saved.getEtapaAtual(),
                usuario, TipoAutor.RH, justificativa != null ? justificativa : "Processo deferido");
        notificarServidor(saved, "Processo deferido",
                "Seu processo " + saved.getProtocolo() + " foi deferido.",
                TipoNotificacao.SOLICITACAO);
        return saved;
    }

    @Override
    public Processo indeferir(Long processoId, String justificativa, String usuario) {
        log.info("Indeferindo processo ID: {}", processoId);
        Processo processo = findProcesso(processoId);
        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalStateException("Justificativa é obrigatória para indeferir.");
        }

        String anterior = processo.getSituacao().name();
        Integer etapaAnterior = processo.getEtapaAtual();
        processo.setSituacao(SituacaoProcesso.INDEFERIDO);
        processo.setResultado(ResultadoProcesso.INDEFERIDO);
        processo.setJustificativaResultado(justificativa);
        processo.setDataConclusao(LocalDateTime.now());
        processo.setDataUltimaAtualizacao(LocalDateTime.now());
        configurarDadosTenant(processo);
        Processo saved = processoRepository.save(processo);
        registrarHistorico(saved, AcaoProcesso.INDEFERIDO, anterior,
                SituacaoProcesso.INDEFERIDO.name(), etapaAnterior, saved.getEtapaAtual(),
                usuario, TipoAutor.RH, justificativa);
        notificarServidor(saved, "Processo indeferido",
                "Seu processo " + saved.getProtocolo() + " foi indeferido. Motivo: " + justificativa,
                TipoNotificacao.ALERTA);
        return saved;
    }

    @Override
    public Processo devolver(Long processoId, String justificativa, String usuario) {
        log.info("Devolvendo processo ID: {} ao servidor", processoId);
        ProcessoComplementacaoRequest request = new ProcessoComplementacaoRequest();
        request.setMotivoConsolidado(justificativa != null ? justificativa : "Revisar informações do processo");
        ProcessoComplementacaoRequest.ItemRequest item = new ProcessoComplementacaoRequest.ItemRequest();
        item.setTipoItem("CAMPO_FORMULARIO");
        item.setLabel("Revisar informações do processo");
        item.setObrigatorio(true);
        item.setMotivo(justificativa);
        item.setOrdem(1);
        request.getItens().add(item);
        return complementacaoService.solicitarComplementacao(processoId, request, usuario != null ? usuario : "RH");
    }

    @Override
    public Processo executar(Long processoId, String usuario) {
        log.info("Marcando processo ID: {} em execução", processoId);
        Processo processo = findProcesso(processoId);

        String anterior = processo.getSituacao().name();
        Integer etapaAnterior = processo.getEtapaAtual();
        processo.setSituacao(SituacaoProcesso.EM_EXECUCAO);
        processo.setDataUltimaAtualizacao(LocalDateTime.now());
        configurarDadosTenant(processo);
        Processo saved = processoRepository.save(processo);
        registrarHistorico(saved, AcaoProcesso.EM_EXECUCAO, anterior,
                SituacaoProcesso.EM_EXECUCAO.name(), etapaAnterior, saved.getEtapaAtual(),
                usuario, TipoAutor.SISTEMA, "Processo em execução automática");
        return saved;
    }

    @Override
    public Processo concluir(Long processoId, String usuario) {
        log.info("Concluindo processo ID: {}", processoId);
        Processo processo = findProcesso(processoId);

        String anterior = processo.getSituacao().name();
        Integer etapaAnterior = processo.getEtapaAtual();
        processo.setSituacao(SituacaoProcesso.CONCLUIDO);
        processo.setDataConclusao(LocalDateTime.now());
        processo.setDataUltimaAtualizacao(LocalDateTime.now());
        configurarDadosTenant(processo);
        Processo saved = processoRepository.save(processo);
        registrarHistorico(saved, AcaoProcesso.CONCLUIDO, anterior,
                SituacaoProcesso.CONCLUIDO.name(), etapaAnterior, saved.getEtapaAtual(),
                usuario, TipoAutor.SISTEMA, "Processo concluído");
        notificarServidor(saved, "Processo concluído",
                "Seu processo " + saved.getProtocolo() + " foi concluído com sucesso.",
                TipoNotificacao.SOLICITACAO);
        return saved;
    }

    @Override
    public Processo arquivar(Long processoId, String usuario) {
        log.info("Arquivando processo ID: {}", processoId);
        Processo processo = findProcesso(processoId);

        String anterior = processo.getSituacao().name();
        Integer etapaAnterior = processo.getEtapaAtual();
        processo.setSituacao(SituacaoProcesso.ARQUIVADO);
        processo.setResultado(ResultadoProcesso.ARQUIVADO);
        processo.setDataConclusao(LocalDateTime.now());
        processo.setDataUltimaAtualizacao(LocalDateTime.now());
        configurarDadosTenant(processo);
        Processo saved = processoRepository.save(processo);
        registrarHistorico(saved, AcaoProcesso.ARQUIVADO, anterior,
                SituacaoProcesso.ARQUIVADO.name(), etapaAnterior, saved.getEtapaAtual(),
                usuario, TipoAutor.RH, "Processo arquivado");
        return saved;
    }

    private void notificarServidor(Processo processo, String titulo, String mensagem, TipoNotificacao tipo) {
        try {
            if (processo.getServidorId() != null) {
                notificacaoService.criarNotificacao(
                        processo.getServidorId(),
                        titulo,
                        mensagem,
                        tipo,
                        "/portal/processos",
                        "/e-RH/portal-servidor/processos/" + processo.getId(),
                        processo.getId(),
                        processo.getEtapaAtual() != null ? "ETAPA_" + processo.getEtapaAtual() : "ABERTURA",
                        "ABRIR_PROCESSO");
            }
        } catch (Exception e) {
            log.warn("Falha ao enviar notificação para servidor do processo {}: {}",
                    processo.getProtocolo(), e.getMessage());
        }
    }

    private Processo findProcesso(Long id) {
        return processoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Processo não encontrado com ID: " + id));
    }

    private void validarTransicao(Processo processo, SituacaoProcesso novaSituacao) {
        SituacaoProcesso atual = processo.getSituacao();
        if (atual == SituacaoProcesso.CONCLUIDO || atual == SituacaoProcesso.CANCELADO ||
                atual == SituacaoProcesso.ARQUIVADO) {
            throw new IllegalStateException(
                    "Processo na situação " + atual.getDescricao() + " não pode ser alterado para " + novaSituacao.getDescricao());
        }
    }

    private void registrarHistorico(Processo processo,
                                    AcaoProcesso acao,
                                    String situacaoAnterior,
                                    String situacaoNova,
                                    Integer etapaAnterior,
                                    Integer etapaNova,
                                    String usuario,
                                    TipoAutor tipoUsuario,
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
        historico.setTipoUsuario(tipoUsuario);
        historico.setDescricao(descricao);
        historico.setUnidadeGestoraId(processo.getUnidadeGestoraId());
        historicoRepository.save(historico);
    }
}
