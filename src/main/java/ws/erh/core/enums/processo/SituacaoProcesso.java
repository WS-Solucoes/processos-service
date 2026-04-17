package ws.erh.core.enums.processo;

import lombok.Getter;

@Getter
public enum SituacaoProcesso {
    RASCUNHO("Rascunho"),
    ABERTO("Aberto"),
    EM_ANALISE("Em Análise"),
    PENDENTE_DOCUMENTACAO("Pendente de Documentação"),
    AGUARDANDO_SUPERIOR("Aguardando Superior"),
    DEFERIDO("Deferido"),
    INDEFERIDO("Indeferido"),
    EM_EXECUCAO("Em Execução"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado"),
    ARQUIVADO("Arquivado");

    private final String descricao;

    SituacaoProcesso(String descricao) {
        this.descricao = descricao;
    }
}
