package ws.processos.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ws.erh.cadastro.portal.service.PortalNotificacaoServiceInterface;
import ws.erh.cadastro.processo.repository.ProcessoRepository;
import ws.erh.core.enums.portal.TipoNotificacao;
import ws.erh.model.cadastro.processo.Processo;

@Service
@RequiredArgsConstructor
public class ProcessoNotificacaoEventAdapter implements PortalNotificacaoServiceInterface {

    private final ProcessoRepository processoRepository;
    private final ProcessoLifecyclePublisher lifecyclePublisher;

    @Override
    public void criarNotificacao(Long servidorId, String titulo, String mensagem, TipoNotificacao tipo, String link) {
        lifecyclePublisher.publishNotification(null, servidorId, titulo, mensagem, tipo, link, null, null, null, null);
    }

    @Override
    public void criarNotificacao(Long servidorId,
                                 String titulo,
                                 String mensagem,
                                 TipoNotificacao tipo,
                                 String link,
                                 String rotaDestino,
                                 Long processoId,
                                 String faseReferencia,
                                 String acaoPrincipal) {
        Processo processo = processoId != null ? processoRepository.findById(processoId).orElse(null) : null;
        lifecyclePublisher.publishNotification(
                processo,
                servidorId,
                titulo,
                mensagem,
                tipo,
                link,
                rotaDestino,
                processoId,
                faseReferencia,
                acaoPrincipal
        );
    }
}
