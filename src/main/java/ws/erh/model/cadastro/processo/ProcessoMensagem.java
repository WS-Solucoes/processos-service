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
import ws.erh.core.enums.processo.TipoAutor;
import ws.erh.model.core.config.AbstractExecucaoTenantEntity;

import java.time.LocalDateTime;

/**
 * Mensagens entre servidor e RH (chat/timeline do processo).
 */
@SQLDelete(sql = "UPDATE processos.processo_mensagem SET excluido = true WHERE id = ?")
@Filter(name = "excluidoFilter", condition = "excluido = :excluido")
@Filter(name = "tenantFilter", condition = "unidade_gestora_id = :unidadeGestoraId")
@Entity
@Table(name = "processo_mensagem", schema = "processos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"processo"})
public class ProcessoMensagem extends AbstractExecucaoTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @Column(name = "autor", nullable = false)
    private String autor;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_autor", nullable = false)
    private TipoAutor tipoAutor;

    @Column(name = "mensagem", columnDefinition = "TEXT", nullable = false)
    private String mensagem;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "lida")
    private Boolean lida = false;

    @Column(name = "data_leitura")
    private LocalDateTime dataLeitura;

    @Column(name = "anexo_nome")
    private String anexoNome;

    @Column(name = "anexo_caminho")
    private String anexoCaminho;

    @Column(name = "anexo_tipo")
    private String anexoTipo;
}
