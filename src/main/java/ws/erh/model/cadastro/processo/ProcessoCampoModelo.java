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
import ws.erh.core.enums.processo.TipoCampo;
import ws.erh.model.core.config.AbstractExecucaoTenantEntity;

/**
 * Campos dinâmicos que o servidor precisa preencher ao abrir o processo.
 */
@SQLDelete(sql = "UPDATE processos.processo_campo_modelo SET excluido = true WHERE id = ?")
@Filter(name = "excluidoFilter", condition = "excluido = :excluido")
@Filter(name = "tenantFilter", condition = "unidade_gestora_id = :unidadeGestoraId")
@Entity
@Table(name = "processo_campo_modelo", schema = "processos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"processoModelo"})
public class ProcessoCampoModelo extends AbstractExecucaoTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_modelo_id", nullable = false)
    private ProcessoModelo processoModelo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_modelo_id")
    private ProcessoEtapaModelo etapaModelo;

    @Column(name = "nome_campo", nullable = false)
    private String nomeCampo;

    @Column(name = "label", nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_campo", nullable = false)
    private TipoCampo tipoCampo;

    @Column(name = "obrigatorio")
    private Boolean obrigatorio = true;

    @Column(name = "opcoes_select")
    private String opcoesSelect;

    @Column(name = "placeholder")
    private String placeholder;

    @Column(name = "ajuda")
    private String ajuda;

    @Column(name = "ordem")
    private Integer ordem = 0;
}
