package ws.erh.cadastro.processo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ws.common.events.processo.ProcessoIntegracaoSolicitadaEvent;
import ws.erh.model.cadastro.processo.Processo;
import ws.processos.event.ProcessoOutboxService;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ProcessoIntegracaoService {

    private final ProcessoOutboxService outboxService;

    public void solicitarIntegracao(Processo processo) {
        if (processo.getProcessoModelo() == null
                || !Boolean.TRUE.equals(processo.getProcessoModelo().getGeraAcaoAutomatica())
                || processo.getProcessoModelo().getCategoria() == null) {
            return;
        }

        ProcessoIntegracaoSolicitadaEvent event = new ProcessoIntegracaoSolicitadaEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setProcessoId(processo.getId());
        event.setProtocolo(processo.getProtocolo());
        event.setUnidadeGestoraId(processo.getUnidadeGestoraId());
        event.setServidorId(processo.getServidorId());
        event.setVinculoFuncionalId(processo.getVinculoFuncionalId());
        event.setCategoria(processo.getProcessoModelo().getCategoria().name());
        event.setDadosFormulario(processo.getDadosFormulario());

        outboxService.enqueueIntegracaoSolicitada(processo.getId(), event);
    }
}
