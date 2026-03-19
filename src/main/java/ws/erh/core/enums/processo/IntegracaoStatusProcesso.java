package ws.erh.core.enums.processo;

import lombok.Getter;

@Getter
public enum IntegracaoStatusProcesso {
    PENDENTE("Pendente"),
    SUCESSO("Sucesso"),
    ERRO("Erro");

    private final String descricao;

    IntegracaoStatusProcesso(String descricao) {
        this.descricao = descricao;
    }
}
