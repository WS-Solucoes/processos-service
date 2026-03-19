package ws.erh.cadastro.processo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ws.erh.cadastro.processo.repository.ProcessoRepository;
import ws.erh.core.enums.processo.IntegracaoStatusProcesso;
import ws.erh.model.cadastro.processo.Processo;

@Service
@Transactional
public class ProcessoIntegracaoOrchestratorService {

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private ProcessoIntegracaoService processoIntegracaoService;

    public Processo executarOuReprocessar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalStateException("Processo nao encontrado para integracao."));

        if (processo.getReferenciaId() != null && processo.getReferenciaTipo() != null
                && processo.getIntegracaoStatus() == IntegracaoStatusProcesso.SUCESSO) {
            return processo;
        }

        processo.setIntegracaoStatus(IntegracaoStatusProcesso.PENDENTE);
        processo.setIntegracaoErro(null);
        processoRepository.save(processo);
        processoIntegracaoService.solicitarIntegracao(processo);
        return processo;
    }
}
