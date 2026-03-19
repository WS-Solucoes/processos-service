package ws.erh.core.enums.processo;

import lombok.Getter;

@Getter
public enum TipoItemComplementacaoProcesso {
    DOCUMENTO_MODELO("Documento do modelo"),
    DOCUMENTO_LIVRE("Documento livre"),
    CAMPO_FORMULARIO("Campo do formulário");

    private final String descricao;

    TipoItemComplementacaoProcesso(String descricao) {
        this.descricao = descricao;
    }
}
