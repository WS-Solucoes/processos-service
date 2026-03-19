package ws.erh.cadastro.processo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProcessoModeloRequest {

    @NotBlank(message = "Código é obrigatório")
    @Size(max = 50, message = "Código deve ter no máximo 50 caracteres")
    private String codigo;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 200, message = "Nome deve ter no máximo 200 caracteres")
    private String nome;

    @Size(max = 1000, message = "Descrição deve ter no máximo 1000 caracteres")
    private String descricao;

    private String instrucoes;

    @NotNull(message = "Categoria é obrigatória")
    private String categoria;

    @Size(max = 50, message = "Ícone deve ter no máximo 50 caracteres")
    private String icone;

    @Size(max = 20, message = "Cor deve ter no máximo 20 caracteres")
    private String cor;

    private Integer prazoAtendimentoDias;

    private Boolean requerAprovacaoChefia;
    private Boolean geraAcaoAutomatica;
    private Boolean ativo;
    private Boolean visivelPortal;
    private Integer ordemExibicao;

    @Valid
    private List<DocumentoModeloRequest> documentosExigidos;

    @Valid
    private List<EtapaModeloRequest> etapas;

    @Valid
    private List<CampoModeloRequest> camposAdicionais;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DocumentoModeloRequest {
        private Long id;
        @NotBlank(message = "Nome do documento é obrigatório")
        private String nome;
        private String descricao;
        private Boolean obrigatorio;
        private String tiposPermitidos;
        private Integer tamanhoMaximoMb;
        private String modeloUrl;
        private Integer ordem;
        private Long etapaModeloId;
        private Integer etapaOrdem;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EtapaModeloRequest {
        private Long id;
        @NotBlank(message = "Nome da etapa é obrigatório")
        private String nome;
        private String descricao;
        @NotNull(message = "Ordem é obrigatória")
        private Integer ordem;
        private String tipoResponsavel;
        private String acaoAutomatica;
        private Integer prazoDias;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CampoModeloRequest {
        private Long id;
        @NotBlank(message = "Nome do campo é obrigatório")
        private String nomeCampo;
        @NotBlank(message = "Label é obrigatório")
        private String label;
        @NotNull(message = "Tipo do campo é obrigatório")
        private String tipoCampo;
        private Boolean obrigatorio;
        private String opcoesSelect;
        private String placeholder;
        private String ajuda;
        private Integer ordem;
        private Long etapaModeloId;
        private Integer etapaOrdem;
    }
}
