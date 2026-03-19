package ws.erh.cadastro.processo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ws.erh.model.cadastro.processo.ProcessoEtapaModelo;

import java.util.List;

@Repository
public interface ProcessoEtapaModeloRepository extends JpaRepository<ProcessoEtapaModelo, Long> {

    @Query("SELECT e FROM ProcessoEtapaModelo e WHERE e.processoModelo.id = :modeloId AND e.excluido = false ORDER BY e.ordem")
    List<ProcessoEtapaModelo> findByProcessoModeloId(@Param("modeloId") Long modeloId);
}
