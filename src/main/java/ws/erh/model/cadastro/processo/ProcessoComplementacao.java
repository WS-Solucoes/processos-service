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
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import ws.erh.core.enums.processo.StatusComplementacaoProcesso;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.core.enums.processo.TipoAutor;
import ws.erh.model.core.config.AbstractExecucaoTenantEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SQLDelete(sql = "UPDATE processos.processo_complementacao SET excluido = true WHERE id = ?")
@Filter(name = "excluidoFilter", condition = "excluido = :excluido")
@Filter(name = "tenantFilter", condition = "unidade_gestora_id = :unidadeGestoraId")
@Entity
@Table(name = "processo_complementacao", schema = "processos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"processo", "itens"})
public class ProcessoComplementacao extends AbstractExecucaoTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @Column(name = "etapa_referencia")
    private Integer etapaReferencia;

    @Column(name = "etapa_nome_snapshot")
    private String etapaNomeSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_retorno", nullable = false)
    private SituacaoProcesso situacaoRetorno;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusComplementacaoProcesso status = StatusComplementacaoProcesso.ABERTA;

    @Column(name = "prazo_limite")
    private LocalDate prazoLimite;

    @Column(name = "motivo_consolidado", columnDefinition = "TEXT")
    private String motivoConsolidado;

    @Column(name = "solicitado_por", nullable = false)
    private String solicitadoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_solicitante", nullable = false)
    private TipoAutor tipoSolicitante = TipoAutor.RH;

    @Column(name = "respondido_por")
    private String respondidoPor;

    @Column(name = "data_solicitacao", nullable = false)
    private LocalDateTime dataSolicitacao;

    @Column(name = "data_resposta")
    private LocalDateTime dataResposta;

    @Column(name = "data_encerramento")
    private LocalDateTime dataEncerramento;

    @OneToMany(mappedBy = "complementacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<ProcessoComplementacaoItem> itens = new ArrayList<>();
}
