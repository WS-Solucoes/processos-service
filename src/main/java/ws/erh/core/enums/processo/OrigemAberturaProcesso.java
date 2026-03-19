package ws.erh.core.enums.processo;

import lombok.Getter;

@Getter
public enum OrigemAberturaProcesso {
    PORTAL_SERVIDOR("Portal do servidor"),
    RH_SOLICITACAO("Solicitação do RH");

    private final String descricao;

    OrigemAberturaProcesso(String descricao) {
        this.descricao = descricao;
    }
}
