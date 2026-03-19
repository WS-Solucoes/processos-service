package ws.erh.core.enums.portal;

import lombok.Getter;

@Getter
public enum TipoNotificacao {
    INFO("Informativo"),
    ALERTA("Alerta"),
    DOCUMENTO("Documento Disponivel"),
    SOLICITACAO("Atualizacao de Solicitacao");

    private final String descricao;

    TipoNotificacao(String descricao) {
        this.descricao = descricao;
    }
}
