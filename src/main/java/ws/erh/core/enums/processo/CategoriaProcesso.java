package ws.erh.core.enums.processo;

import lombok.Getter;

@Getter
public enum CategoriaProcesso {
    FERIAS("Férias"),
    AFASTAMENTO("Afastamento / Licença"),
    LICENCA("Licença Específica"),
    RESCISAO("Desligamento / Exoneração"),
    CADASTRAL("Atualização Cadastral"),
    FINANCEIRO("Assunto Financeiro"),
    DOCUMENTAL("Solicitação de Documento"),
    LIBERACAO_LOTE_PAGAMENTO("Liberação de Lote de Pagamento"),
    OUTROS("Outros");

    private final String descricao;

    CategoriaProcesso(String descricao) {
        this.descricao = descricao;
    }
}
