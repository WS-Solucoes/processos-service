package ws.erh.cadastro.processo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ws.erh.cadastro.processo.repository.ProcessoRepository;
import ws.erh.core.enums.processo.CategoriaProcesso;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.model.cadastro.processo.Processo;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Tarefa agendada para auto-arquivamento de processos inativos.
 * Processos com situação CONCLUIDO ou CANCELADO há mais de 90 dias são automaticamente arquivados.
 * Processos com situação PENDENTE_DOCUMENTACAO sem atualização há mais de 30 dias são arquivados.
 *
 * <p>S5.10 — também aplica SLA por categoria a processos em situações ativas
 * ({@code EM_ANALISE}, {@code EM_EXECUCAO}, {@code AGUARDANDO_SUPERIOR}). Quando o
 * processo ultrapassa o limite configurado para sua categoria, é automaticamente
 * arquivado com justificativa explícita, evitando que casos órfãos permaneçam
 * bloqueando o painel de RH (ex.: eRH indisponível).</p>
 */
@Component
@Slf4j
public class ProcessoAutoArquivamentoScheduler {

    /**
     * SLA (em dias) por categoria de processo, aplicado às situações ativas.
     * Categorias críticas (FERIAS, RESCISAO) têm SLA mais curto; categorias
     * genéricas/financeiras toleram mais tempo.
     */
    static final Map<CategoriaProcesso, Integer> SLA_DIAS_POR_CATEGORIA;

    /** Situações consideradas ativas para fins de SLA. */
    static final List<SituacaoProcesso> SITUACOES_ATIVAS = List.of(
            SituacaoProcesso.ABERTO,
            SituacaoProcesso.EM_ANALISE,
            SituacaoProcesso.EM_EXECUCAO,
            SituacaoProcesso.AGUARDANDO_SUPERIOR);

    /** SLA padrão usado quando a categoria não tem entrada explícita. */
    static final int SLA_DIAS_PADRAO = 60;

    static {
        SLA_DIAS_POR_CATEGORIA = new EnumMap<>(CategoriaProcesso.class);
        SLA_DIAS_POR_CATEGORIA.put(CategoriaProcesso.FERIAS, 30);
        SLA_DIAS_POR_CATEGORIA.put(CategoriaProcesso.AFASTAMENTO, 45);
        SLA_DIAS_POR_CATEGORIA.put(CategoriaProcesso.LICENCA, 45);
        SLA_DIAS_POR_CATEGORIA.put(CategoriaProcesso.RESCISAO, 30);
        SLA_DIAS_POR_CATEGORIA.put(CategoriaProcesso.CADASTRAL, 60);
        SLA_DIAS_POR_CATEGORIA.put(CategoriaProcesso.FINANCEIRO, 90);
        SLA_DIAS_POR_CATEGORIA.put(CategoriaProcesso.DOCUMENTAL, 60);
        SLA_DIAS_POR_CATEGORIA.put(CategoriaProcesso.OUTROS, SLA_DIAS_PADRAO);
    }

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private ProcessoGestaoServiceInterface processoGestaoService;

    @Autowired
    private ConfiguracaoAutoArquivamentoService configService;

    /**
     * Executa diariamente às 02:00 da manhã.
     * Arquiva processos concluídos ou cancelados há mais de 90 dias,
     * processos pendentes de documentação sem atualização há mais de 30 dias,
     * e processos em situações ativas que ultrapassaram o SLA da sua categoria.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoArquivarProcessos() {
        var cfg = configService.obterOuCriarPadrao();
        if (Boolean.FALSE.equals(cfg.getHabilitado())) {
            log.info("Auto-arquivamento desabilitado por configuração — pulando execução");
            return;
        }
        log.info("Iniciando auto-arquivamento de processos inativos...");

        int totalArquivados = 0;

        // 1. Arquivar processos concluídos há mais de N dias (configurável)
        totalArquivados += arquivarPorSituacao(
                SituacaoProcesso.CONCLUIDO, cfg.getDiasConcluido(),
                "Auto-arquivado: processo concluído há mais de " + cfg.getDiasConcluido() + " dias");

        // 2. Arquivar processos cancelados há mais de N dias (configurável)
        totalArquivados += arquivarPorSituacao(
                SituacaoProcesso.CANCELADO, cfg.getDiasCancelado(),
                "Auto-arquivado: processo cancelado há mais de " + cfg.getDiasCancelado() + " dias");

        // 3. Pendentes de documentação
        totalArquivados += arquivarPendentesInativos(cfg.getDiasPendenteDocumentacao());

        // 4. S5.10 - SLA por categoria sobre situações ativas
        totalArquivados += arquivarPorSlaCategoria();

        log.info("Auto-arquivamento concluído. Total de processos arquivados: {}", totalArquivados);
    }

    /**
     * Arquiva processos de uma situação específica que não foram atualizados dentro do prazo.
     */
    int arquivarPorSituacao(SituacaoProcesso situacao, int diasInativos, String justificativa) {
        LocalDateTime limite = LocalDateTime.now().minusDays(diasInativos);
        List<Processo> processos = processoRepository.findBySituacao(situacao);

        int count = 0;
        for (Processo processo : processos) {
            LocalDateTime ultimaAtualizacao = processo.getDataUltimaAtualizacao() != null
                    ? processo.getDataUltimaAtualizacao()
                    : processo.getDataAbertura();

            if (ultimaAtualizacao != null && ultimaAtualizacao.isBefore(limite)) {
                try {
                    processoGestaoService.arquivar(processo.getId(), "SISTEMA");
                    count++;
                    log.debug("Processo {} arquivado automaticamente - {}", processo.getProtocolo(), justificativa);
                } catch (Exception e) {
                    log.warn("Erro ao auto-arquivar processo {}: {}", processo.getProtocolo(), e.getMessage());
                }
            }
        }
        return count;
    }

    /**
     * Arquiva processos pendentes de documentação que não foram atualizados dentro do prazo.
     */
    int arquivarPendentesInativos(int diasInativos) {
        LocalDateTime limite = LocalDateTime.now().minusDays(diasInativos);
        List<Processo> processos = processoRepository.findBySituacao(SituacaoProcesso.PENDENTE_DOCUMENTACAO);

        int count = 0;
        for (Processo processo : processos) {
            LocalDateTime ultimaAtualizacao = processo.getDataUltimaAtualizacao() != null
                    ? processo.getDataUltimaAtualizacao()
                    : processo.getDataAbertura();

            if (ultimaAtualizacao != null && ultimaAtualizacao.isBefore(limite)) {
                try {
                    processoGestaoService.arquivar(processo.getId(), "SISTEMA");
                    count++;
                    log.debug("Processo {} arquivado automaticamente - Pendente de documentação há mais de {} dias",
                            processo.getProtocolo(), diasInativos);
                } catch (Exception e) {
                    log.warn("Erro ao auto-arquivar processo {}: {}", processo.getProtocolo(), e.getMessage());
                }
            }
        }
        return count;
    }

    /**
     * S5.10 — Arquiva processos em situações ativas cujo tempo desde a última
     * atualização ultrapassa o SLA configurado para sua categoria.
     *
     * @return total de processos arquivados por estouro de SLA
     */
    int arquivarPorSlaCategoria() {
        int count = 0;
        LocalDateTime agora = LocalDateTime.now();
        for (SituacaoProcesso situacao : SITUACOES_ATIVAS) {
            for (Processo processo : processoRepository.findBySituacao(situacao)) {
                CategoriaProcesso categoria = processo.getProcessoModelo() != null
                        ? processo.getProcessoModelo().getCategoria() : null;
                int slaDias = configService.slaDiasPara(categoria);

                LocalDateTime referencia = processo.getDataUltimaAtualizacao() != null
                        ? processo.getDataUltimaAtualizacao()
                        : processo.getDataAbertura();
                if (referencia == null) {
                    continue;
                }
                long dias = java.time.Duration.between(referencia, agora).toDays();
                if (dias <= slaDias) {
                    continue;
                }
                try {
                    processoGestaoService.arquivar(processo.getId(), "SISTEMA");
                    count++;
                    log.info("S5.10: processo {} (categoria {}) arquivado por SLA estourado: {} dias > {} dias",
                            processo.getProtocolo(), categoria, dias, slaDias);
                } catch (Exception e) {
                    log.warn("S5.10: erro ao arquivar processo {} por SLA: {}",
                            processo.getProtocolo(), e.getMessage());
                }
            }
        }
        return count;
    }
}
