package ws.erh.cadastro.processo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ws.common.config.SpringApplicationContext;
import ws.erh.cadastro.processo.service.ProcessoValidacaoService;
import ws.erh.cadastro.processo.service.ProcessoWorkflowService;
import ws.erh.model.cadastro.processo.Processo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class ProcessoResponse {

    private Long id;
    private String protocolo;

    private Long processoModeloId;
    private String processoModeloCodigo;
    private String processoModeloNome;
    private String processoModeloIcone;
    private String processoModeloCor;
    private String processoModeloCategoria;
    private String processoModeloCategoriaDescricao;

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

    private Integer totalDocumentos;
    private Integer totalMensagens;
    private Integer mensagensNaoLidas;

    private List<String> acoesDisponiveis = new ArrayList<>();
    private List<ProcessoPendenciaDocumentalResponse> pendenciasDocumentais = new ArrayList<>();
    private List<ProcessoDocumentoResponse> documentos = new ArrayList<>();
    private List<ProcessoMensagemResponse> mensagens = new ArrayList<>();
    private List<ProcessoHistoricoResponse> historico = new ArrayList<>();

    public ProcessoResponse(Processo entity) {
        this(entity, false);
    }

    public ProcessoResponse(Processo entity, boolean incluirDetalhes) {
        ProcessoWorkflowService workflowService = SpringApplicationContext.getBean(ProcessoWorkflowService.class);
        ProcessoValidacaoService validacaoService = SpringApplicationContext.getBean(ProcessoValidacaoService.class);

        this.id = entity.getId();
        this.protocolo = entity.getProtocolo();
        this.etapaAtual = entity.getEtapaAtual();
        this.atribuidoPara = entity.getAtribuidoPara();
        this.departamentoAtribuido = entity.getDepartamentoAtribuido();
        this.dadosFormulario = entity.getDadosFormulario();
        this.observacaoServidor = entity.getObservacaoServidor();
        this.justificativaResultado = entity.getJustificativaResultado();
        this.referenciaTipo = entity.getReferenciaTipo();
        this.referenciaId = entity.getReferenciaId();
        this.integracaoErro = entity.getIntegracaoErro();

        if (entity.getSituacao() != null) {
            this.situacao = entity.getSituacao().name();
            this.situacaoDescricao = entity.getSituacao().getDescricao();
        }
        if (entity.getResultado() != null) {
            this.resultado = entity.getResultado().name();
            this.resultadoDescricao = entity.getResultado().getDescricao();
        }
        if (entity.getPrioridade() != null) {
            this.prioridade = entity.getPrioridade().name();
            this.prioridadeDescricao = entity.getPrioridade().getDescricao();
        }
        if (entity.getOrigemAbertura() != null) {
            this.origemAbertura = entity.getOrigemAbertura().name();
            this.origemAberturaDescricao = entity.getOrigemAbertura().getDescricao();
        }
        if (entity.getIntegracaoStatus() != null) {
            this.integracaoStatus = entity.getIntegracaoStatus().name();
            this.integracaoStatusDescricao = entity.getIntegracaoStatus().getDescricao();
        }

        if (entity.getDataAbertura() != null) {
            this.dataAbertura = entity.getDataAbertura().toString();
        }
        if (entity.getDataUltimaAtualizacao() != null) {
            this.dataUltimaAtualizacao = entity.getDataUltimaAtualizacao().toString();
        }
        if (entity.getDataConclusao() != null) {
            this.dataConclusao = entity.getDataConclusao().toString();
        }
        if (entity.getPrazoLimite() != null) {
            this.prazoLimite = entity.getPrazoLimite().toString();
        }

        try {
            if (entity.getProcessoModelo() != null) {
                this.processoModeloId = entity.getProcessoModelo().getId();
                this.processoModeloCodigo = entity.getProcessoModelo().getCodigo();
                this.processoModeloNome = entity.getProcessoModelo().getNome();
                this.processoModeloIcone = entity.getProcessoModelo().getIcone();
                this.processoModeloCor = entity.getProcessoModelo().getCor();
                if (entity.getProcessoModelo().getCategoria() != null) {
                    this.processoModeloCategoria = entity.getProcessoModelo().getCategoria().name();
                    this.processoModeloCategoriaDescricao = entity.getProcessoModelo().getCategoria().getDescricao();
                }
            }
        } catch (Exception ignored) {
        }

        try {
            this.servidorId = entity.getServidorId();
            this.servidorNome = entity.getServidorNome();
            this.servidorCpf = entity.getServidorCpf();
        } catch (Exception ignored) {
        }

        try {
            this.vinculoFuncionalId = entity.getVinculoFuncionalId();
            this.vinculoFuncionalMatricula = entity.getVinculoFuncionalMatricula();
        } catch (Exception ignored) {
        }

        try {
            this.totalEtapas = workflowService.resolveTotalEtapas(entity);
            this.etapaAtualNome = workflowService.resolveEtapaAtualNome(entity);
            this.responsavelAtual = workflowService.resolveResponsavelAtual(entity);
            this.podeComplementar = workflowService.podeComplementar(entity);
            this.acoesDisponiveis = new ArrayList<>(workflowService.resolveAcoesDisponiveis(entity));
        } catch (Exception ignored) {
        }

        try {
            this.pendenciasDocumentais = validacaoService.calcularPendencias(entity);
        } catch (Exception ignored) {
        }

        try {
            if (entity.getDocumentos() != null) {
                this.totalDocumentos = entity.getDocumentos().size();
            }
        } catch (Exception ignored) {
            this.totalDocumentos = 0;
        }

        try {
            if (entity.getMensagens() != null) {
                this.totalMensagens = entity.getMensagens().size();
                this.mensagensNaoLidas = (int) entity.getMensagens().stream()
                        .filter(m -> m.getLida() == null || !m.getLida())
                        .count();
            }
        } catch (Exception ignored) {
            this.totalMensagens = 0;
            this.mensagensNaoLidas = 0;
        }

        if (incluirDetalhes) {
            try {
                if (entity.getDocumentos() != null) {
                    this.documentos = entity.getDocumentos().stream()
                            .map(ProcessoDocumentoResponse::new)
                            .collect(Collectors.toList());
                }
            } catch (Exception ignored) {
            }

            try {
                if (entity.getMensagens() != null) {
                    this.mensagens = entity.getMensagens().stream()
                            .map(ProcessoMensagemResponse::new)
                            .collect(Collectors.toList());
                }
            } catch (Exception ignored) {
            }

            try {
                if (entity.getHistorico() != null) {
                    this.historico = entity.getHistorico().stream()
                            .map(ProcessoHistoricoResponse::new)
                            .collect(Collectors.toList());
                }
            } catch (Exception ignored) {
            }
        }
    }
}
