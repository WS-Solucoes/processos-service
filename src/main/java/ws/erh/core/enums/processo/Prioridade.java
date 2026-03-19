package ws.erh.core.enums.processo;

import lombok.Getter;

@Getter
public enum Prioridade {
    BAIXA("Baixa"),
    NORMAL("Normal"),
    ALTA("Alta"),
    URGENTE("Urgente");

    private final String descricao;

    Prioridade(String descricao) {
        this.descricao = descricao;
    }
}
