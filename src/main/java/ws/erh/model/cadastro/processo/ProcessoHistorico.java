package ws.erh.model.cadastro.processo;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ws.erh.core.enums.processo.AcaoProcesso;
import ws.erh.core.enums.processo.TipoAutor;

import java.time.LocalDateTime;

/**
 * Log de auditoria de tudo que aconteceu no processo (timeline de eventos).
 * Não usa soft delete — registros são imutáveis.
 */
@Entity
@Table(name = "processo_historico", schema = "processos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"processo"})
public class ProcessoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "acao", nullable = false)
    private AcaoProcesso acao;

    @Column(name = "situacao_anterior")
    private String situacaoAnterior;

    @Column(name = "situacao_nova")
    private String situacaoNova;

    @Column(name = "etapa_anterior")
    private Integer etapaAnterior;

    @Column(name = "etapa_nova")
    private Integer etapaNova;

    @Column(name = "usuario", nullable = false)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario")
    private TipoAutor tipoUsuario;

    @Column(name = "descricao")
    private String descricao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados_extras", columnDefinition = "JSONB")
    private String dadosExtras;

    @Column(name = "unidade_gestora_id")
    private Long unidadeGestoraId;
}
