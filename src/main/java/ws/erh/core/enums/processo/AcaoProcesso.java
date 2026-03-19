package ws.erh.core.enums.processo;

import lombok.Getter;

@Getter
public enum AcaoProcesso {
    CRIADO("Processo criado"),
    DOCUMENTO_ENVIADO("Documento enviado"),
    DOCUMENTO_ACEITO("Documento aceito"),
    DOCUMENTO_RECUSADO("Documento recusado"),
    DOCUMENTACAO_SOLICITADA("Documentação solicitada"),
    MENSAGEM_SERVIDOR("Mensagem do servidor"),
    MENSAGEM_RH("Mensagem do RH"),
    MENSAGEM_CHEFIA("Mensagem da chefia"),
    SOLICITADO_COMPLEMENTO("Complementação solicitada"),
    ATRIBUIDO("Processo atribuído"),
    EM_ANALISE("Em análise"),
    ETAPA_AVANCADA("Etapa avançada"),
    ENCAMINHADO_CHEFIA("Encaminhado para chefia"),
    APROVADO_CHEFIA("Aprovado pela chefia"),
    REPROVADO_CHEFIA("Reprovado pela chefia"),
    DEFERIDO("Processo deferido"),
    INDEFERIDO("Processo indeferido"),
    DEVOLVIDO("Processo devolvido"),
    EM_EXECUCAO("Em execução"),
    ACAO_EXECUTADA("Ação executada"),
    CONCLUIDO("Processo concluído"),
    CANCELADO("Processo cancelado"),
    REABERTO("Processo reaberto"),
    ARQUIVADO("Processo arquivado");

    private final String descricao;

    AcaoProcesso(String descricao) {
        this.descricao = descricao;
    }
}
