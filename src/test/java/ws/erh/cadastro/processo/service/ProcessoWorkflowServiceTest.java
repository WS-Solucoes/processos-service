package ws.erh.cadastro.processo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.core.enums.processo.TipoResponsavel;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoEtapaModelo;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProcessoWorkflowService - Testes unitarios")
class ProcessoWorkflowServiceTest {

    private final ProcessoWorkflowService service = new ProcessoWorkflowService();

    @Test
    @DisplayName("Deve iniciar solicitacao RH em pendencia documental")
    void deveIniciarSolicitacaoRhEmPendenciaDocumental() {
        Processo processo = buildProcesso();

        service.prepararParaSolicitacaoRh(processo);

        assertEquals(SituacaoProcesso.PENDENTE_DOCUMENTACAO, processo.getSituacao());
        assertEquals(1, processo.getEtapaAtual());
        assertEquals("SERVIDOR", service.resolveResponsavelAtual(processo));
        assertTrue(service.podeComplementar(processo));
    }

    @Test
    @DisplayName("Deve avancar para RH quando servidor submeter etapa inicial")
    void deveAvancarParaRhQuandoServidorSubmeterEtapaInicial() {
        Processo processo = buildProcesso();

        service.prepararParaSubmissaoServidor(processo);

        assertEquals(2, processo.getEtapaAtual());
        assertEquals(SituacaoProcesso.ABERTO, processo.getSituacao());
        assertEquals("RH", service.resolveResponsavelAtual(processo));
    }

    @Test
    @DisplayName("Deve colocar processo em analise quando RH iniciar")
    void deveColocarProcessoEmAnaliseQuandoRhIniciar() {
        Processo processo = buildProcesso();
        processo.setEtapaAtual(2);
        processo.setSituacao(SituacaoProcesso.ABERTO);

        service.iniciarAnalise(processo);

        assertEquals(SituacaoProcesso.EM_ANALISE, processo.getSituacao());
        assertEquals("RH", service.resolveResponsavelAtual(processo));
        assertFalse(service.podeComplementar(processo));
    }

    @Test
    @DisplayName("Deve encaminhar para chefia usando proxima etapa de chefia")
    void deveEncaminharParaChefiaUsandoProximaEtapaDeChefia() {
        Processo processo = buildProcesso();
        processo.setEtapaAtual(2);
        processo.setSituacao(SituacaoProcesso.EM_ANALISE);

        service.encaminharChefia(processo);

        assertEquals(3, processo.getEtapaAtual());
        assertEquals(SituacaoProcesso.AGUARDANDO_CHEFIA, processo.getSituacao());
        assertEquals("CHEFIA", service.resolveResponsavelAtual(processo));
    }

    @Test
    @DisplayName("Deve retomar para RH apos complementacao do servidor")
    void deveRetomarParaRhAposComplementacaoDoServidor() {
        Processo processo = buildProcesso();
        processo.setEtapaAtual(1);
        processo.setSituacao(SituacaoProcesso.PENDENTE_DOCUMENTACAO);

        service.retomarAposComplementacao(processo);

        assertEquals(2, processo.getEtapaAtual());
        assertEquals(SituacaoProcesso.ABERTO, processo.getSituacao());
        assertFalse(service.podeComplementar(processo));
    }

    @Test
    @DisplayName("Deve expor acoes derivadas do workflow")
    void deveExporAcoesDerivadasDoWorkflow() {
        Processo processo = buildProcesso();
        processo.setSituacao(SituacaoProcesso.DEFERIDO);

        List<String> acoes = service.resolveAcoesDisponiveis(processo);

        assertTrue(acoes.contains("REPROCESSAR_INTEGRACAO"));
        assertTrue(acoes.contains("ARQUIVAR"));
    }

    private Processo buildProcesso() {
        ProcessoModelo modelo = new ProcessoModelo();
        modelo.setEtapas(new ArrayList<>(List.of(
                etapa("Complementacao do Servidor", 1, TipoResponsavel.SERVIDOR),
                etapa("Analise RH", 2, TipoResponsavel.RH),
                etapa("Aprovacao da Chefia", 3, TipoResponsavel.CHEFIA)
        )));

        Processo processo = new Processo();
        processo.setProcessoModelo(modelo);
        processo.setEtapaAtual(1);
        processo.setSituacao(SituacaoProcesso.RASCUNHO);
        return processo;
    }

    private ProcessoEtapaModelo etapa(String nome, Integer ordem, TipoResponsavel tipoResponsavel) {
        ProcessoEtapaModelo etapa = new ProcessoEtapaModelo();
        etapa.setNome(nome);
        etapa.setOrdem(ordem);
        etapa.setTipoResponsavel(tipoResponsavel);
        return etapa;
    }
}
