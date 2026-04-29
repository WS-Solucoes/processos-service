package ws.erh.model.cadastro.processo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuração de auto-arquivamento de processos (S7.10).
 *
 * <p>Singleton (id=1). Permite sobrescrever valores hardcoded do
 * {@code ProcessoAutoArquivamentoScheduler}.</p>
 */
@Entity
@Table(name = "configuracao_auto_arquivamento", schema = "processos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracaoAutoArquivamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "habilitado", nullable = false)
    private Boolean habilitado = true;

    @Column(name = "dias_concluido", nullable = false)
    private Integer diasConcluido = 90;

    @Column(name = "dias_cancelado", nullable = false)
    private Integer diasCancelado = 90;

    @Column(name = "dias_pendente_documentacao", nullable = false)
    private Integer diasPendenteDocumentacao = 30;

    @Column(name = "sla_dias_padrao", nullable = false)
    private Integer slaDiasPadrao = 60;

    @Column(name = "sla_ferias", nullable = false)
    private Integer slaFerias = 30;

    @Column(name = "sla_afastamento", nullable = false)
    private Integer slaAfastamento = 45;

    @Column(name = "sla_licenca", nullable = false)
    private Integer slaLicenca = 45;

    @Column(name = "sla_rescisao", nullable = false)
    private Integer slaRescisao = 30;

    @Column(name = "sla_cadastral", nullable = false)
    private Integer slaCadastral = 60;

    @Column(name = "sla_financeiro", nullable = false)
    private Integer slaFinanceiro = 90;

    @Column(name = "sla_documental", nullable = false)
    private Integer slaDocumental = 60;

    @Column(name = "sla_outros", nullable = false)
    private Integer slaOutros = 60;
}
