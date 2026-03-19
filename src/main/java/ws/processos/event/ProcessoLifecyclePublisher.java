package ws.processos.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ws.common.events.processo.ProcessoEventType;
import ws.common.events.processo.ProcessoLifecycleEvent;
import ws.erh.core.enums.portal.TipoNotificacao;
import ws.erh.model.cadastro.processo.Processo;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcessoLifecyclePublisher {

    private final ProcessoOutboxService outboxService;

    public void publishNotification(Processo processo,
                                    Long servidorId,
                                    String titulo,
                                    String mensagem,
                                    TipoNotificacao tipoNotificacao,
                                    String link,
                                    String rotaDestino,
                                    Long processoId,
                                    String faseReferencia,
                                    String acaoPrincipal) {
        ProcessoLifecycleEvent event = new ProcessoLifecycleEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setProcessoId(processoId != null ? processoId : (processo != null ? processo.getId() : null));
        event.setProtocolo(processo != null ? processo.getProtocolo() : null);
        event.setUnidadeGestoraId(processo != null ? processo.getUnidadeGestoraId() : null);
        event.setServidorId(servidorId);
        event.setServidorNome(processo != null ? processo.getServidorNome() : null);
        event.setEventType(resolveEventType(titulo, acaoPrincipal).name());
        event.setTipo(tipoNotificacao != null ? tipoNotificacao.name() : null);
        event.setTitulo(titulo);
        event.setMensagem(mensagem);
        event.setLink(link);
        event.setRotaDestino(rotaDestino);
        event.setFaseReferencia(faseReferencia);
        event.setAcaoPrincipal(acaoPrincipal);
        event.setReferenciaId(processo != null ? processo.getReferenciaId() : null);
        event.setReferenciaTipo(processo != null ? processo.getReferenciaTipo() : null);
        event.setCategoria(processo != null && processo.getProcessoModelo() != null
                && processo.getProcessoModelo().getCategoria() != null
                ? processo.getProcessoModelo().getCategoria().name()
                : null);
        event.setEtapaAtual(processo != null ? processo.getEtapaAtual() : null);

        if (event.getProcessoId() != null) {
            outboxService.enqueueLifecycle(event.getProcessoId(), event);
        }
    }

    private ProcessoEventType resolveEventType(String titulo, String acaoPrincipal) {
        if ("COMPLEMENTAR".equalsIgnoreCase(acaoPrincipal)) {
            return ProcessoEventType.COMPLEMENTACAO_SOLICITADA;
        }
        if (titulo != null && titulo.toLowerCase().contains("conclu")) {
            return ProcessoEventType.CONCLUIDO;
        }
        if (titulo != null && titulo.toLowerCase().contains("indefer")) {
            return ProcessoEventType.INDEFERIDO;
        }
        if (titulo != null && titulo.toLowerCase().contains("defer")) {
            return ProcessoEventType.DEFERIDO;
        }
        return ProcessoEventType.MENSAGEM_ENVIADA;
    }
}
