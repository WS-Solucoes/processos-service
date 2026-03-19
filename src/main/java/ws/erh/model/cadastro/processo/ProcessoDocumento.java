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
import ws.erh.core.enums.processo.SituacaoDocumento;
import ws.erh.model.core.config.AbstractExecucaoTenantEntity;

import java.time.LocalDateTime;

/**
 * Documento enviado pelo servidor (ou pelo RH) em um processo.
 */
@SQLDelete(sql = "UPDATE processos.processo_documento SET excluido = true WHERE id = ?")
@Filter(name = "excluidoFilter", condition = "excluido = :excluido")
@Filter(name = "tenantFilter", condition = "unidade_gestora_id = :unidadeGestoraId")
@Entity
@Table(name = "processo_documento", schema = "processos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"processo", "documentoModelo"})
public class ProcessoDocumento extends AbstractExecucaoTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_modelo_id")
    private ProcessoDocumentoModelo documentoModelo;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Column(name = "caminho_storage", nullable = false)
    private String caminhoStorage;

    @Column(name = "tipo_arquivo")
    private String tipoArquivo;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio;

    @Column(name = "enviado_por", nullable = false)
    private String enviadoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao")
    private SituacaoDocumento situacao = SituacaoDocumento.PENDENTE;

    @Column(name = "motivo_recusa")
    private String motivoRecusa;

    @Column(name = "avaliado_por")
    private String avaliadoPor;

    @Column(name = "data_avaliacao")
    private LocalDateTime dataAvaliacao;
}
