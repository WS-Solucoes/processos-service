package ws.erh.cadastro.processo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ws.erh.cadastro.processo.repository.ProcessoHistoricoRepository;
import ws.erh.cadastro.processo.repository.ProcessoRepository;
import ws.erh.core.base.AbstractTenantService;
import ws.erh.core.enums.processo.AcaoProcesso;
import ws.erh.core.enums.processo.IntegracaoStatusProcesso;
import ws.erh.core.enums.processo.OrigemAberturaProcesso;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.core.enums.processo.TipoAutor;
import ws.erh.core.exception.EntityNotFoundException;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoHistorico;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@Slf4j
public class ProcessoService extends AbstractTenantService implements ProcessoServiceInterface {

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private ProcessoHistoricoRepository historicoRepository;

    @Autowired
    private ProcessoWorkflowService workflowService;

    @Autowired
    private ProcessoSnapshotEnrichmentService snapshotEnrichmentService;

    @Override
    public Processo abrirProcesso(Processo processo) {
        log.info("Abrindo processo para servidor ID: {}", processo.getServidorId());
        return criarProcesso(
                processo,
                OrigemAberturaProcesso.PORTAL_SERVIDOR,
                "Servidor",
                TipoAutor.SERVIDOR,
                "Processo submetido via portal",
                false);
    }

    @Override
    public Processo solicitarProcesso(Processo processo, String usuario, String descricaoInicial) {
        log.info("Solicitando processo para servidor ID: {}", processo.getServidorId());
        return criarProcesso(
                processo,
                OrigemAberturaProcesso.RH_SOLICITACAO,
                usuario != null ? usuario : "RH",
                TipoAutor.RH,
                descricaoInicial != null && !descricaoInicial.isBlank()
                        ? descricaoInicial
                        : "Processo solicitado pelo RH para complementação do servidor",
                true);
    }

    @Override
    @Transactional(readOnly = true)
    public Processo findById(Long id) {
        return processoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Processo não encontrado com ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Processo findByIdAndServidorId(Long id, Long servidorId) {
        return processoRepository.findByIdAndServidorId(id, servidorId)
                .orElseThrow(() -> new EntityNotFoundException("Processo não encontrado para o servidor informado."));
    }

    @Override
    @Transactional(readOnly = true)
    public Processo findByProtocolo(String protocolo) {
        return processoRepository.findByProtocolo(protocolo)
                .orElseThrow(() -> new EntityNotFoundException("Processo não encontrado com protocolo: " + protocolo));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Processo> getAll(Pageable pageable) {
        return processoRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Processo> findByServidorId(Long servidorId, Pageable pageable) {
        return processoRepository.findByServidorId(servidorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Processo> findBySituacao(SituacaoProcesso situacao) {
        return processoRepository.findBySituacao(situacao);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Processo> findVencidos() {
        return processoRepository.findVencidos();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Processo> findPendentes(Pageable pageable) {
        List<SituacaoProcesso> pendentes = Arrays.asList(
                SituacaoProcesso.ABERTO,
                SituacaoProcesso.EM_ANALISE,
                SituacaoProcesso.PENDENTE_DOCUMENTACAO,
                SituacaoProcesso.AGUARDANDO_CHEFIA
        );
        return processoRepository.findBySituacaoIn(pendentes, pageable);
    }

    @Override
    public Processo cancelar(Long id, String justificativa, String usuario) {
        log.info("Cancelando processo ID: {}", id);
        Processo processo = findById(id);

        if (processo.getSituacao() == SituacaoProcesso.CONCLUIDO ||
                processo.getSituacao() == SituacaoProcesso.CANCELADO) {
            throw new IllegalStateException("Processo na situação " + processo.getSituacao().getDescricao() + " não pode ser cancelado.");
        }

        String situacaoAnterior = processo.getSituacao().name();
        Integer etapaAnterior = processo.getEtapaAtual();
        processo.setSituacao(SituacaoProcesso.CANCELADO);
        processo.setDataConclusao(LocalDateTime.now());
        processo.setDataUltimaAtualizacao(LocalDateTime.now());

        configurarDadosTenant(processo);
        Processo saved = processoRepository.save(processo);

        registrarHistorico(saved, AcaoProcesso.CANCELADO, situacaoAnterior,
                SituacaoProcesso.CANCELADO.name(), etapaAnterior, etapaAnterior,
                usuario, TipoAutor.SERVIDOR,
                justificativa != null ? justificativa : "Processo cancelado");

        return saved;
    }

    @Override
    public void deleteProcesso(Long id) {
        log.info("Excluindo processo ID: {}", id);
        Processo processo = findById(id);
        if (processo.getSituacao() != SituacaoProcesso.RASCUNHO) {
            throw new IllegalStateException("Apenas processos em rascunho podem ser excluídos.");
        }
        processoRepository.delete(processo);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("rascunhos", processoRepository.countBySituacao(SituacaoProcesso.RASCUNHO));
        dashboard.put("abertos", processoRepository.countBySituacao(SituacaoProcesso.ABERTO));
        dashboard.put("emAnalise", processoRepository.countBySituacao(SituacaoProcesso.EM_ANALISE));
        dashboard.put("aguardandoServidor", processoRepository.countBySituacao(SituacaoProcesso.PENDENTE_DOCUMENTACAO));
        dashboard.put("pendentes", processoRepository.countBySituacao(SituacaoProcesso.PENDENTE_DOCUMENTACAO));
        dashboard.put("aguardandoChefia", processoRepository.countBySituacao(SituacaoProcesso.AGUARDANDO_CHEFIA));
        dashboard.put("deferidos", processoRepository.countBySituacao(SituacaoProcesso.DEFERIDO));
        dashboard.put("indeferidos", processoRepository.countBySituacao(SituacaoProcesso.INDEFERIDO));
        dashboard.put("emExecucao", processoRepository.countBySituacao(SituacaoProcesso.EM_EXECUCAO));
        dashboard.put("concluidos", processoRepository.countBySituacao(SituacaoProcesso.CONCLUIDO));
        dashboard.put("cancelados", processoRepository.countBySituacao(SituacaoProcesso.CANCELADO));
        dashboard.put("vencidos", processoRepository.findVencidos().size());

        LocalDateTime inicioMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime fimMes = inicioMes.plusMonths(1).minusSeconds(1);
        dashboard.put("abertosNoMes", processoRepository.countByPeriodo(inicioMes, fimMes));

        return dashboard;
    }

    @Override
    public String gerarProtocolo() {
        int ano = Year.now().getValue();
        String prefixo = "PROC-" + ano + "-";
        int sequencial = processoRepository.findMaxSequencialByAno(prefixo) + 1;
        return String.format("PROC-%d-%06d", ano, sequencial);
    }

    private Processo criarProcesso(Processo processo,
                                   OrigemAberturaProcesso origemAbertura,
                                   String usuarioHistorico,
                                   TipoAutor tipoAutor,
                                   String descricaoHistorico,
                                   boolean solicitacaoRh) {
        snapshotEnrichmentService.enrich(processo);
        processo.setProtocolo(gerarProtocolo());
        processo.setOrigemAbertura(origemAbertura);
        processo.setIntegracaoStatus(IntegracaoStatusProcesso.PENDENTE);
        processo.setIntegracaoErro(null);
        processo.setDataAbertura(LocalDateTime.now());
        processo.setDataUltimaAtualizacao(LocalDateTime.now());
        processo.setDataConclusao(null);

        ProcessoModelo modelo = processo.getProcessoModelo();
        if (processo.getPrazoLimite() == null && modelo != null && modelo.getPrazoAtendimentoDias() != null) {
            processo.setPrazoLimite(LocalDate.now().plusDays(modelo.getPrazoAtendimentoDias()));
        }

        if (solicitacaoRh) {
            workflowService.prepararParaSolicitacaoRh(processo);
        } else {
            workflowService.prepararParaSubmissaoServidor(processo);
        }

        configurarDadosTenant(processo);
        Processo saved = processoRepository.save(processo);

        registrarHistorico(saved, AcaoProcesso.CRIADO, null,
                saved.getSituacao() != null ? saved.getSituacao().name() : null,
                null, saved.getEtapaAtual(),
                usuarioHistorico, tipoAutor, descricaoHistorico);

        log.info("Processo criado com protocolo {} e situação {}", saved.getProtocolo(), saved.getSituacao());
        return saved;
    }

    private void registrarHistorico(Processo processo, AcaoProcesso acao,
                                    String situacaoAnterior, String situacaoNova,
                                    Integer etapaAnterior, Integer etapaNova,
                                    String usuario, TipoAutor tipoUsuario,
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
