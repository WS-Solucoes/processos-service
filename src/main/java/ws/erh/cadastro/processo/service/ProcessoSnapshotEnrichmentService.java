package ws.erh.cadastro.processo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ws.erh.model.cadastro.processo.Processo;
import ws.processos.client.ErhSnapshotClient;
import ws.processos.client.ServidorSnapshotResponse;
import ws.processos.client.VinculoSnapshotResponse;

@Service
@RequiredArgsConstructor
public class ProcessoSnapshotEnrichmentService {

    private final ErhSnapshotClient snapshotClient;

    public void enrich(Processo processo) {
        Long servidorId = processo.getServidorId();
        if (servidorId == null) {
            throw new IllegalStateException("Servidor obrigatorio para abertura do processo.");
        }

        ServidorSnapshotResponse servidor = snapshotClient.getServidor(servidorId);
        processo.setServidorId(servidor.getId());
        processo.setServidorNome(servidor.getNome());
        processo.setServidorCpf(servidor.getCpf());
        processo.setUnidadeGestoraNome(servidor.getUnidadeGestoraNome());
        processo.setMunicipioNome(servidor.getMunicipioNome());
        if (servidor.getUnidadeGestoraId() != null) {
            processo.setUnidadeGestoraId(servidor.getUnidadeGestoraId());
        }

        Long vinculoId = processo.getVinculoFuncionalId();
        if (vinculoId != null) {
            VinculoSnapshotResponse vinculo = snapshotClient.getVinculo(vinculoId);
            processo.setVinculoFuncionalId(vinculo.getId());
            processo.setVinculoFuncionalMatricula(vinculo.getMatricula());
        }
    }
}
