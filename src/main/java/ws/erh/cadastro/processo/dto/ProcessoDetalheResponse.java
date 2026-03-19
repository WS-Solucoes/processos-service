package ws.erh.cadastro.processo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProcessoDetalheResponse {

    private Long id;
    private String protocolo;
    private String processoModeloNome;
    private String processoModeloCategoria;
    private String processoModeloCategoriaDescricao;
    private String processoModeloIcone;
    private String processoModeloCor;
    private Long servidorId;
    private String servidorNome;
    private String servidorCpf;
    private Long vinculoFuncionalId;
    private String vinculoFuncionalMatricula;
    private String situacao;
    private String situacaoDescricao;
    private Integer etapaAtual;
    private String etapaAtualNome;
    private Integer totalEtapas;
    private String responsavelAtual;
    private Boolean podeComplementar;
    private String dataAbertura;
    private String dataUltimaAtualizacao;
    private String dataConclusao;
    private String prazoLimite;
    private String atribuidoPara;
    private String departamentoAtribuido;
    private String dadosFormulario;
    private String observacaoServidor;
    private String origemAbertura;
    private String origemAberturaDescricao;
    private String resultado;
    private String resultadoDescricao;
    private String justificativaResultado;
    private String referenciaTipo;
    private Long referenciaId;
    private String integracaoStatus;
    private String integracaoStatusDescricao;
    private String integracaoErro;
    private String prioridade;
    private String prioridadeDescricao;

    private List<String> acoesDisponiveis = new ArrayList<>();
    private List<ProcessoPendenciaDocumentalResponse> pendenciasDocumentais = new ArrayList<>();
    private List<FaseResponse> fases = new ArrayList<>();
    private FaseResponse faseAtualDetalhe;
    private List<TimelineItemResponse> timeline = new ArrayList<>();
    private List<ComplementacaoResponse> complementacoes = new ArrayList<>();
    private List<FormularioCampoResponse> formularioEstruturado = new ArrayList<>();
    private List<DocumentoFaseResponse> documentosPorFase = new ArrayList<>();
    private List<ProcessoMensagemResponse> mensagens = new ArrayList<>();
    private List<ProcessoHistoricoResponse> historico = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FaseResponse {
        private String chave;
        private Long etapaModeloId;
        private Integer ordem;
        private String nome;
        private String descricao;
        private String tipoResponsavel;
        private String status;
        private Boolean atual = false;
        private List<FormularioCampoResponse> formulario = new ArrayList<>();
        private List<DocumentoFaseResponse> documentos = new ArrayList<>();
        private ComplementacaoResponse complementacaoAtiva;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TimelineItemResponse {
        private String chave;
        private Integer ordem;
        private String nome;
        private String tipoResponsavel;
        private String status;
        private Boolean atual = false;
        private String resumo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class FormularioCampoResponse {
        private Long campoModeloId;
        private Long etapaModeloId;
        private Integer etapaOrdem;
        private String etapaNome;
        private String nomeCampo;
        private String label;
        private String tipoCampo;
        private Boolean obrigatorio;
        private String placeholder;
        private String ajuda;
        private Integer ordem;
        private String valor;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DocumentoFaseResponse {
        private Long documentoModeloId;
        private Long etapaModeloId;
        private Integer etapaOrdem;
        private String etapaNome;
        private String nomeDocumento;
        private String descricao;
        private Boolean obrigatorio;
        private Integer ordem;
        private ProcessoPendenciaDocumentalResponse pendencia;
        private ProcessoDocumentoResponse ultimoDocumento;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ComplementacaoResponse {
        private Long id;
        private Integer etapaReferencia;
        private String etapaNome;
        private String situacaoRetorno;
        private String situacaoRetornoDescricao;
        private String status;
        private String statusDescricao;
        private String prazoLimite;
        private String motivoConsolidado;
        private String solicitadoPor;
        private String tipoSolicitante;
        private String respondidoPor;
        private String dataSolicitacao;
        private String dataResposta;
        private String dataEncerramento;
        private Boolean ativa = false;
        private List<ComplementacaoItemResponse> itens = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ComplementacaoItemResponse {
        private Long id;
        private String tipoItem;
        private String tipoItemDescricao;
        private Long documentoModeloId;
        private Long campoModeloId;
        private String label;
        private Boolean obrigatorio;
        private String motivo;
        private Integer ordem;
        private Long documentoRespondidoId;
        private String documentoRespondidoNome;
    }
}
