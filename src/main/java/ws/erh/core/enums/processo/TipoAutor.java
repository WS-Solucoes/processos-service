package ws.erh.core.enums.processo;

import lombok.Getter;

@Getter
public enum TipoAutor {
    SERVIDOR("Servidor"),
    RH("RH"),
    SUPERIOR("Superior"),
    SISTEMA("Sistema");

    private final String descricao;

    TipoAutor(String descricao) {
        this.descricao = descricao;
    }
}
