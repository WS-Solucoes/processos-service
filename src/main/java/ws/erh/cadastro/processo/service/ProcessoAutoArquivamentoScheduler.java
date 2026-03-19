package ws.erh.cadastro.processo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ws.erh.cadastro.processo.repository.ProcessoRepository;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.model.cadastro.processo.Processo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tarefa agendada para auto-arquivamento de processos inativos.
 * Processos com situação CONCLUIDO ou CANCELADO há mais de 90 dias são automaticamente arquivados.
 * Processos com situação PENDENTE_DOCUMENTACAO sem atualização há mais de 30 dias são arquivados.
 */
@Component
@Slf4j
public class ProcessoAutoArquivamentoScheduler {

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private ProcessoGestaoServiceInterface processoGestaoService;

    /**
     * Executa diariamente às 02:00 da manhã.
     * Arquiva processos concluídos ou cancelados há mais de 90 dias,
     * e processos pendentes de documentação sem atualização há mais de 30 dias.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoArquivarProcessos() {
        log.info("Iniciando auto-arquivamento de processos inativos...");

        int totalArquivados = 0;

        // 1. Arquivar processos concluídos há mais de 90 dias
        totalArquivados += arquivarPorSituacao(
                SituacaoProcesso.CONCLUIDO, 90,
                "Auto-arquivado: processo concluído há mais de 90 dias");

        // 2. Arquivar processos cancelados há mais de 90 dias
        totalArquivados += arquivarPorSituacao(
                SituacaoProcesso.CANCELADO, 90,
                "Auto-arquivado: processo cancelado há mais de 90 dias");

        // 3. Arquivar processos pendentes de documentação sem atualização há mais de 30 dias
        totalArquivados += arquivarPendentesInativos(30);

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
}
