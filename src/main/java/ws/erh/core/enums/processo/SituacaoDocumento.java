package ws.erh.core.enums.processo;

import lombok.Getter;

@Getter
public enum SituacaoDocumento {
    PENDENTE("Pendente"),
    ACEITO("Aceito"),
    RECUSADO("Recusado");

    private final String descricao;

    SituacaoDocumento(String descricao) {
        this.descricao = descricao;
    }
}
