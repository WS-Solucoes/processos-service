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
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SQLDelete;
import ws.erh.core.enums.processo.TipoItemComplementacaoProcesso;
import ws.erh.model.core.config.AbstractExecucaoTenantEntity;

@SQLDelete(sql = "UPDATE processos.processo_complementacao_item SET excluido = true WHERE id = ?")
@Filter(name = "excluidoFilter", condition = "excluido = :excluido")
@Filter(name = "tenantFilter", condition = "unidade_gestora_id = :unidadeGestoraId")
@Entity
@Table(name = "processo_complementacao_item", schema = "processos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"complementacao", "documentoModelo", "campoModelo", "documentoRespondido"})
public class ProcessoComplementacaoItem extends AbstractExecucaoTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complementacao_id", nullable = false)
    private ProcessoComplementacao complementacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_item", nullable = false)
    private TipoItemComplementacaoProcesso tipoItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_modelo_id")
    private ProcessoDocumentoModelo documentoModelo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campo_modelo_id")
    private ProcessoCampoModelo campoModelo;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "obrigatorio")
    private Boolean obrigatorio = true;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "ordem")
    private Integer ordem = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_respondido_id")
    private ProcessoDocumento documentoRespondido;
}
