package ws.erh.cadastro.processo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ws.erh.model.cadastro.processo.ProcessoDocumentoModelo;

import java.util.List;

@Repository
public interface ProcessoDocumentoModeloRepository extends JpaRepository<ProcessoDocumentoModelo, Long> {

    @Query("SELECT d FROM ProcessoDocumentoModelo d WHERE d.processoModelo.id = :modeloId AND d.excluido = false ORDER BY d.ordem")
    List<ProcessoDocumentoModelo> findByProcessoModeloId(@Param("modeloId") Long modeloId);
}
