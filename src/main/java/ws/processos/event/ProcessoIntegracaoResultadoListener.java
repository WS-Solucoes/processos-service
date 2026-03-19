package ws.processos.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ws.common.events.processo.ProcessoIntegracaoResultadoEvent;
import ws.erh.cadastro.processo.repository.ProcessoRepository;
import ws.erh.cadastro.processo.service.ProcessoGestaoServiceInterface;
import ws.erh.core.enums.processo.IntegracaoStatusProcesso;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.model.cadastro.processo.Processo;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessoIntegracaoResultadoListener {

    private final ProcessoRepository processoRepository;
    private final ProcessoGestaoServiceInterface processoGestaoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(queues = "${processos.rabbit.integracao-resultado-queue}")
    @Transactional
    public void onMessage(String payload) {
        try {
            ProcessoIntegracaoResultadoEvent event = objectMapper.readValue(payload, ProcessoIntegracaoResultadoEvent.class);
            Processo processo = processoRepository.findById(event.getProcessoId())
                    .orElseThrow(() -> new IllegalStateException("Processo nao encontrado para retorno de integracao."));

            if ("SUCESSO".equalsIgnoreCase(event.getStatus())) {
                processo.setReferenciaTipo(event.getReferenciaTipo());
                processo.setReferenciaId(event.getReferenciaId());
                processo.setIntegracaoErro(null);
                processo.setIntegracaoStatus(IntegracaoStatusProcesso.SUCESSO);
                processoRepository.save(processo);
                if (processo.getSituacao() != SituacaoProcesso.CONCLUIDO
                        && processo.getSituacao() != SituacaoProcesso.CANCELADO
                        && processo.getSituacao() != SituacaoProcesso.ARQUIVADO) {
                    processoGestaoService.executar(processo.getId(), "SISTEMA");
                    processoGestaoService.concluir(processo.getId(), "SISTEMA");
                }
            } else {
                processo.setIntegracaoStatus(IntegracaoStatusProcesso.ERRO);
                processo.setIntegracaoErro(event.getErro());
                processoRepository.save(processo);
            }
        } catch (Exception e) {
            log.error("Falha ao processar retorno de integracao: {}", e.getMessage(), e);
            throw new IllegalStateException("Falha ao processar retorno de integracao.", e);
        }
    }
}
