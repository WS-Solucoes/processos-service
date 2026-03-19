package ws.processos.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ws.erh.cadastro.processo.repository.ProcessoRepository;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.processos.dto.ProcessoResumoResponse;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProcessoResumoService {

    private static final List<SituacaoProcesso> SITUACOES_FINALIZADAS = List.of(
            SituacaoProcesso.CONCLUIDO,
            SituacaoProcesso.CANCELADO,
            SituacaoProcesso.ARQUIVADO
    );

    private final ProcessoRepository processoRepository;

    public ProcessoResumoResponse getResumo(Long servidorId, Long unidadeGestoraId) {
        long processosAbertos = processoRepository.countProcessosAbertos(
                servidorId,
                unidadeGestoraId,
                SITUACOES_FINALIZADAS
        );
        return new ProcessoResumoResponse(servidorId, unidadeGestoraId, processosAbertos);
    }
}
