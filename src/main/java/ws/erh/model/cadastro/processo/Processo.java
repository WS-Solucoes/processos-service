package ws.erh.model.cadastro.processo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.type.SqlTypes;
import ws.erh.core.enums.processo.IntegracaoStatusProcesso;
import ws.erh.core.enums.processo.OrigemAberturaProcesso;
import ws.erh.core.enums.processo.Prioridade;
import ws.erh.core.enums.processo.ResultadoProcesso;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.model.cadastro.servidor.Servidor;
import ws.erh.model.cadastro.vinculo.VinculoFuncional;
import ws.erh.model.core.config.AbstractExecucaoTenantEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Instancia de um processo aberto por um servidor.
 * Contem o protocolo, situacao, etapa atual, documentos enviados, mensagens e historico.
 */
@SQLDelete(sql = "UPDATE processos.processo SET excluido = true WHERE id = ?")
@Filter(name = "excluidoFilter", condition = "excluido = :excluido")
@Filter(name = "tenantFilter", condition = "unidade_gestora_id = :unidadeGestoraId")
@Entity
@Table(name = "processo", schema = "processos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"processoModelo", "documentos", "mensagens", "historico"})
public class Processo extends AbstractExecucaoTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "protocolo", nullable = false, unique = true)
    private String protocolo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_modelo_id", nullable = false)
    private ProcessoModelo processoModelo;

    @Column(name = "servidor_id", nullable = false)
    private Long servidorId;

    @Column(name = "servidor_nome")
    private String servidorNome;

    @Column(name = "servidor_cpf")
    private String servidorCpf;

    @Transient
    private Servidor servidor;

    @Column(name = "vinculo_funcional_id")
    private Long vinculoFuncionalId;

    @Column(name = "vinculo_funcional_matricula")
    private String vinculoFuncionalMatricula;

    @Transient
    private VinculoFuncional vinculoFuncional;

    @Column(name = "unidade_gestora_nome")
    private String unidadeGestoraNome;

    @Column(name = "municipio_nome")
    private String municipioNome;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao", nullable = false)
    private SituacaoProcesso situacao = SituacaoProcesso.ABERTO;

    @Column(name = "etapa_atual")
    private Integer etapaAtual = 1;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_ultima_atualizacao")
    private LocalDateTime dataUltimaAtualizacao;

    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;

    @Column(name = "prazo_limite")
    private LocalDate prazoLimite;

    @Column(name = "atribuido_para")
    private String atribuidoPara;

    @Column(name = "departamento_atribuido")
    private String departamentoAtribuido;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados_formulario", columnDefinition = "JSONB")
    private String dadosFormulario;

    @Column(name = "observacao_servidor", columnDefinition = "TEXT")
    private String observacaoServidor;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_abertura", nullable = false)
    private OrigemAberturaProcesso origemAbertura = OrigemAberturaProcesso.PORTAL_SERVIDOR;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado")
    private ResultadoProcesso resultado;

    @Column(name = "justificativa_resultado", columnDefinition = "TEXT")
    private String justificativaResultado;

    @Column(name = "referencia_tipo")
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "integracao_status", nullable = false)
    private IntegracaoStatusProcesso integracaoStatus = IntegracaoStatusProcesso.PENDENTE;

    @Column(name = "integracao_erro", columnDefinition = "TEXT")
    private String integracaoErro;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade")
    private Prioridade prioridade = Prioridade.NORMAL;

    @OneToMany(mappedBy = "processo", cascade = CascadeType.ALL)
    @OrderBy("dataEnvio ASC")
    private List<ProcessoDocumento> documentos = new ArrayList<>();

    @OneToMany(mappedBy = "processo", cascade = CascadeType.ALL)
    @OrderBy("dataHora ASC")
    private List<ProcessoMensagem> mensagens = new ArrayList<>();

    @OneToMany(mappedBy = "processo", cascade = CascadeType.ALL)
    @OrderBy("dataHora ASC")
    private List<ProcessoHistorico> historico = new ArrayList<>();

    public void setServidor(Servidor servidor) {
        this.servidor = servidor;
        this.servidorId = servidor != null ? servidor.getId() : null;
        if (servidor != null) {
            if (servidor.getNome() != null && !servidor.getNome().isBlank()) {
                this.servidorNome = servidor.getNome();
            }
            if (servidor.getCpf() != null && !servidor.getCpf().isBlank()) {
                this.servidorCpf = servidor.getCpf();
            }
        }
    }

    public void setVinculoFuncional(VinculoFuncional vinculoFuncional) {
        this.vinculoFuncional = vinculoFuncional;
        this.vinculoFuncionalId = vinculoFuncional != null ? vinculoFuncional.getId() : null;
        if (vinculoFuncional != null && vinculoFuncional.getMatricula() != null
                && !vinculoFuncional.getMatricula().isBlank()) {
            this.vinculoFuncionalMatricula = vinculoFuncional.getMatricula();
        }
    }
}
