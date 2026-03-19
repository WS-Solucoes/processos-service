package ws.erh.core.enums.processo;

import lombok.Getter;

@Getter
public enum ResultadoProcesso {
    DEFERIDO("Deferido"),
    INDEFERIDO("Indeferido"),
    ARQUIVADO("Arquivado");

    private final String descricao;

    ResultadoProcesso(String descricao) {
        this.descricao = descricao;
    }
}
