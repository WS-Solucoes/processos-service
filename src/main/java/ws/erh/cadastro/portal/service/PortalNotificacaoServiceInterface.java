package ws.erh.cadastro.portal.service;

import ws.erh.core.enums.portal.TipoNotificacao;

public interface PortalNotificacaoServiceInterface {

    void criarNotificacao(Long servidorId, String titulo, String mensagem, TipoNotificacao tipo, String link);

    void criarNotificacao(Long servidorId,
                          String titulo,
                          String mensagem,
                          TipoNotificacao tipo,
                          String link,
                          String rotaDestino,
                          Long processoId,
                          String faseReferencia,
                          String acaoPrincipal);
}
