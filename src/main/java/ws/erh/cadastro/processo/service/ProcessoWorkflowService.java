package ws.erh.cadastro.processo.service;

import org.springframework.stereotype.Service;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.core.enums.processo.TipoResponsavel;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoEtapaModelo;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ProcessoWorkflowService {

    public void prepararParaSubmissaoServidor(Processo processo) {
        ensureEtapaAtual(processo);
        if (getTipoResponsavelEtapaAtual(processo) == TipoResponsavel.SERVIDOR) {
            advanceToNextEtapa(processo);
        }
        aplicarSituacaoDaEtapaAtual(processo, false);
    }

    public void prepararParaSolicitacaoRh(Processo processo) {
        ensureEtapaAtual(processo);
        processo.setSituacao(SituacaoProcesso.PENDENTE_DOCUMENTACAO);
    }

    public void retomarAposComplementacao(Processo processo) {
        ensureEtapaAtual(processo);
        if (getTipoResponsavelEtapaAtual(processo) == TipoResponsavel.SERVIDOR) {
            advanceToNextEtapa(processo);
        }
        aplicarSituacaoDaEtapaAtual(processo, false);
    }

    public void iniciarAnalise(Processo processo) {
        ensureEtapaAtual(processo);
        if (getTipoResponsavelEtapaAtual(processo) == TipoResponsavel.SERVIDOR) {
            advanceToNextEtapa(processo);
        }
        aplicarSituacaoDaEtapaAtual(processo, true);
    }

    public void solicitarComplementacao(Processo processo) {
        ensureEtapaAtual(processo);
        processo.setSituacao(SituacaoProcesso.PENDENTE_DOCUMENTACAO);
    }

    public void encaminharChefia(Processo processo) {
        ensureEtapaAtual(processo);
        ProcessoEtapaModelo proximaChefia = findNextEtapa(processo, TipoResponsavel.CHEFIA);
        if (proximaChefia != null) {
            processo.setEtapaAtual(proximaChefia.getOrdem());
        }
        processo.setSituacao(SituacaoProcesso.AGUARDANDO_CHEFIA);
    }

    public String resolveEtapaAtualNome(Processo processo) {
        ProcessoEtapaModelo etapaAtual = getEtapaAtual(processo);
        return etapaAtual != null ? etapaAtual.getNome() : null;
    }

    public Integer resolveTotalEtapas(Processo processo) {
        return getEtapasOrdenadas(processo.getProcessoModelo()).size();
    }

    public String resolveResponsavelAtual(Processo processo) {
        if (processo == null || processo.getSituacao() == null) {
            return "RH";
        }

        return switch (processo.getSituacao()) {
            case RASCUNHO, PENDENTE_DOCUMENTACAO -> "SERVIDOR";
            case AGUARDANDO_CHEFIA -> "CHEFIA";
            case EM_EXECUCAO -> "SISTEMA";
            case DEFERIDO, INDEFERIDO, CANCELADO, CONCLUIDO, ARQUIVADO -> "FINALIZADO";
            default -> {
                TipoResponsavel responsavel = getTipoResponsavelEtapaAtual(processo);
                yield responsavel != null ? responsavel.name() : "RH";
            }
        };
    }

    public boolean podeComplementar(Processo processo) {
        if (processo == null || processo.getSituacao() == null) {
            return false;
        }
        return processo.getSituacao() == SituacaoProcesso.PENDENTE_DOCUMENTACAO
                || processo.getSituacao() == SituacaoProcesso.RASCUNHO;
    }

    public List<String> resolveAcoesDisponiveis(Processo processo) {
        if (processo == null || processo.getSituacao() == null) {
            return List.of();
        }

        return switch (processo.getSituacao()) {
            case RASCUNHO -> List.of("SUBMETER", "CANCELAR");
            case PENDENTE_DOCUMENTACAO -> List.of("COMPLEMENTAR", "CANCELAR", "ENVIAR_MENSAGEM");
            case ABERTO -> List.of("ATRIBUIR", "ANALISAR", "SOLICITAR_DOCUMENTACAO", "ENVIAR_MENSAGEM");
            case EM_ANALISE -> List.of("SOLICITAR_DOCUMENTACAO", "ENCAMINHAR_CHEFIA", "DEFERIR", "INDEFERIR", "DEVOLVER", "ENVIAR_MENSAGEM");
            case AGUARDANDO_CHEFIA -> List.of("DEFERIR", "INDEFERIR", "DEVOLVER", "ENVIAR_MENSAGEM");
            case DEFERIDO -> List.of("REPROCESSAR_INTEGRACAO", "ARQUIVAR");
            case EM_EXECUCAO -> List.of("ACOMPANHAR_EXECUCAO");
            case CONCLUIDO -> List.of("ARQUIVAR");
            default -> List.of();
        };
    }

    public TipoResponsavel getTipoResponsavelEtapaAtual(Processo processo) {
        ProcessoEtapaModelo etapaAtual = getEtapaAtual(processo);
        return etapaAtual != null ? etapaAtual.getTipoResponsavel() : null;
    }

    public ProcessoEtapaModelo getEtapaAtual(Processo processo) {
        if (processo == null) {
            return null;
        }
        Integer ordemAtual = processo.getEtapaAtual();
        if (ordemAtual == null) {
            return null;
        }
        return getEtapasOrdenadas(processo.getProcessoModelo()).stream()
                .filter(etapa -> ordemAtual.equals(etapa.getOrdem()))
                .findFirst()
                .orElse(null);
    }

    private void ensureEtapaAtual(Processo processo) {
        if (processo.getEtapaAtual() != null) {
            return;
        }
        List<ProcessoEtapaModelo> etapas = getEtapasOrdenadas(processo.getProcessoModelo());
        processo.setEtapaAtual(etapas.isEmpty() ? 1 : etapas.get(0).getOrdem());
    }

    private void aplicarSituacaoDaEtapaAtual(Processo processo, boolean analysisStarted) {
        TipoResponsavel responsavel = getTipoResponsavelEtapaAtual(processo);
        if (responsavel == null) {
            processo.setSituacao(analysisStarted ? SituacaoProcesso.EM_ANALISE : SituacaoProcesso.ABERTO);
            return;
        }

        processo.setSituacao(situacaoParaResponsavel(responsavel, analysisStarted));
    }

    private boolean advanceToNextEtapa(Processo processo) {
        List<ProcessoEtapaModelo> etapas = getEtapasOrdenadas(processo.getProcessoModelo());
        if (etapas.isEmpty()) {
            return false;
        }

        Integer etapaAtual = processo.getEtapaAtual();
        if (etapaAtual == null) {
            processo.setEtapaAtual(etapas.get(0).getOrdem());
            return true;
        }

        for (ProcessoEtapaModelo etapa : etapas) {
            if (etapa.getOrdem() != null && etapa.getOrdem() > etapaAtual) {
                processo.setEtapaAtual(etapa.getOrdem());
                return true;
            }
        }
        return false;
    }

    private ProcessoEtapaModelo findNextEtapa(Processo processo, TipoResponsavel tipoResponsavel) {
        Integer etapaAtual = processo.getEtapaAtual();
        return getEtapasOrdenadas(processo.getProcessoModelo()).stream()
                .filter(etapa -> etapa.getTipoResponsavel() == tipoResponsavel)
                .filter(etapa -> etapaAtual == null || etapa.getOrdem() > etapaAtual)
                .findFirst()
                .orElse(null);
    }

    private SituacaoProcesso situacaoParaResponsavel(TipoResponsavel tipoResponsavel, boolean analysisStarted) {
        return switch (tipoResponsavel) {
            case SERVIDOR -> SituacaoProcesso.PENDENTE_DOCUMENTACAO;
            case CHEFIA -> SituacaoProcesso.AGUARDANDO_CHEFIA;
            case RH -> analysisStarted ? SituacaoProcesso.EM_ANALISE : SituacaoProcesso.ABERTO;
        };
    }

    private List<ProcessoEtapaModelo> getEtapasOrdenadas(ProcessoModelo modelo) {
        if (modelo == null || modelo.getEtapas() == null) {
            return List.of();
        }

        List<ProcessoEtapaModelo> etapas = new ArrayList<>(modelo.getEtapas());
        etapas.sort(Comparator.comparing(ProcessoEtapaModelo::getOrdem, Comparator.nullsLast(Integer::compareTo)));
        return etapas;
    }
}
