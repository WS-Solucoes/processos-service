package ws.erh.model.cadastro.processo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import ws.erh.core.enums.processo.CategoriaProcesso;
import ws.erh.model.core.config.AbstractExecucaoTenantEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Template configurado pelo RH: define que tipo de processo pode ser aberto pelos servidores.
 * Exemplos: "Solicitação de Férias", "Licença para Tratamento de Saúde", etc.
 */
@SQLDelete(sql = "UPDATE processos.processo_modelo SET excluido = true WHERE id = ?")
@Filter(name = "excluidoFilter", condition = "excluido = :excluido")
@Filter(name = "tenantFilter", condition = "unidade_gestora_id = :unidadeGestoraId")
@Entity
@Table(name = "processo_modelo", schema = "processos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, exclude = {"documentosExigidos", "etapas", "camposAdicionais"})
public class ProcessoModelo extends AbstractExecucaoTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "codigo", nullable = false)
    private String codigo;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "instrucoes", columnDefinition = "TEXT")
    private String instrucoes;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false)
    private CategoriaProcesso categoria;

    @Column(name = "icone")
    private String icone;

    @Column(name = "cor")
    private String cor;

    @Column(name = "prazo_atendimento_dias")
    private Integer prazoAtendimentoDias;

    @Column(name = "requer_aprovacao_chefia")
    private Boolean requerAprovacaoChefia = false;

    @Column(name = "gera_acao_automatica")
    private Boolean geraAcaoAutomatica = false;

    @Column(name = "ativo")
    private Boolean ativo = true;

    @Column(name = "visivel_portal")
    private Boolean visivelPortal = true;

    @Column(name = "ordem_exibicao")
    private Integer ordemExibicao = 0;

    @OneToMany(mappedBy = "processoModelo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<ProcessoDocumentoModelo> documentosExigidos = new ArrayList<>();

    @OneToMany(mappedBy = "processoModelo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<ProcessoEtapaModelo> etapas = new ArrayList<>();

    @OneToMany(mappedBy = "processoModelo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<ProcessoCampoModelo> camposAdicionais = new ArrayList<>();
}
