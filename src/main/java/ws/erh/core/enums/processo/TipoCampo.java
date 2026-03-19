package ws.erh.core.enums.processo;

import lombok.Getter;

@Getter
public enum TipoCampo {
    TEXT("Texto"),
    NUMBER("Número"),
    DATE("Data"),
    SELECT("Seleção"),
    BOOLEAN("Sim/Não"),
    TEXTAREA("Texto Longo");

    private final String descricao;

    TipoCampo(String descricao) {
        this.descricao = descricao;
    }
}
