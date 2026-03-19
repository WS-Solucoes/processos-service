package ws.erh.cadastro.processo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ws.erh.core.enums.processo.CategoriaProcesso;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.util.List;

public interface ProcessoModeloServiceInterface {

    ProcessoModelo saveProcessoModelo(ProcessoModelo modelo);
    ProcessoModelo updateProcessoModelo(ProcessoModelo modelo);
    ProcessoModelo findById(Long id);
    ProcessoModelo findByCodigo(String codigo);
    Page<ProcessoModelo> getAll(Pageable pageable);
    List<ProcessoModelo> findAtivos();
    List<ProcessoModelo> findVisivelPortal();
    List<ProcessoModelo> findByCategoria(CategoriaProcesso categoria);
    void deleteProcessoModelo(Long id);
}
