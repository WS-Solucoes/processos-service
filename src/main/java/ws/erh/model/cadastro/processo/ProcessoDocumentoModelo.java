package ws.erh.model.cadastro.processo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import ws.erh.model.core.config.AbstractExecucaoTenantEntity;

/**
 * Define quais documentos são exigidos para determinado tipo de processo.
 */
@SQLDelete(sql = "UPDATE processos.processo_documento_modelo SET excluido = true WHERE id = ?")
@Filter(name = "excluidoFilter", condition = "excluido = :excluido")
@Filter(name = "tenantFilter", condition = "unidade_gestora_id = :unidadeGestoraId")
@Entity
@Table(name = "processo_documento_modelo", schema = "processos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"processoModelo"})
public class ProcessoDocumentoModelo extends AbstractExecucaoTenantEntity {

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

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "descricao")
    private String descricao;

    @Column(name = "obrigatorio")
    private Boolean obrigatorio = true;

    @Column(name = "tipos_permitidos")
    private String tiposPermitidos;

    @Column(name = "tamanho_maximo_mb")
    private Integer tamanhoMaximoMb = 10;

    @Column(name = "modelo_url")
    private String modeloUrl;

    @Column(name = "ordem")
    private Integer ordem = 0;
}
