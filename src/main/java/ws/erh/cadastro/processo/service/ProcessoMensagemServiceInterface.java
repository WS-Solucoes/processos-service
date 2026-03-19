package ws.erh.cadastro.processo.service;

import ws.erh.model.cadastro.processo.ProcessoMensagem;

import java.util.List;

public interface ProcessoMensagemServiceInterface {

    ProcessoMensagem enviarMensagem(ProcessoMensagem mensagem);
    List<ProcessoMensagem> findByProcessoId(Long processoId);
    long countNaoLidas(Long processoId);
    void marcarComoLidas(Long processoId);
}
