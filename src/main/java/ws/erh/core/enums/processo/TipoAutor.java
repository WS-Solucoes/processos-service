package ws.erh.core.enums.processo;

import lombok.Getter;

@Getter
public enum TipoAutor {
    SERVIDOR("Servidor"),
    RH("RH"),
    CHEFIA("Chefia"),
    SISTEMA("Sistema");

    private final String descricao;

    TipoAutor(String descricao) {
        this.descricao = descricao;
    }
}
