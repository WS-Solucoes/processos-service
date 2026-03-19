package ws.processos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "erh-service", url = "${processos.erh-service.url:}")
public interface ErhSnapshotClient {

    @GetMapping("/api/v1/processo-snapshots/servidores/{id}")
    ServidorSnapshotResponse getServidor(@PathVariable("id") Long id);

    @GetMapping("/api/v1/processo-snapshots/vinculos/{id}")
    VinculoSnapshotResponse getVinculo(@PathVariable("id") Long id);
}
