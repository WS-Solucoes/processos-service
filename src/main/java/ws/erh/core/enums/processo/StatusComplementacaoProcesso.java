package ws.erh.core.enums.processo;

import lombok.Getter;

@Getter
public enum StatusComplementacaoProcesso {
    ABERTA("Aberta"),
    RESPONDIDA("Respondida"),
    ENCERRADA("Encerrada"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusComplementacaoProcesso(String descricao) {
        this.descricao = descricao;
    }
}
