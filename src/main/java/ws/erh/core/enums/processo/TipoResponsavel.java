package ws.erh.core.enums.processo;

import lombok.Getter;

@Getter
public enum TipoResponsavel {
    SERVIDOR("Servidor"),
    RH("RH"),
    CHEFIA("Chefia");

    private final String descricao;

    TipoResponsavel(String descricao) {
        this.descricao = descricao;
    }
}
