package ws.erh.cadastro.processo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ws.erh.model.cadastro.processo.ProcessoHistorico;

import java.util.List;

@Repository
public interface ProcessoHistoricoRepository extends JpaRepository<ProcessoHistorico, Long> {

    @Query("SELECT h FROM ProcessoHistorico h WHERE h.processo.id = :processoId ORDER BY h.dataHora ASC")
    List<ProcessoHistorico> findByProcessoId(@Param("processoId") Long processoId);

    @Query("SELECT h FROM ProcessoHistorico h WHERE h.processo.id = :processoId ORDER BY h.dataHora DESC")
    List<ProcessoHistorico> findByProcessoIdDesc(@Param("processoId") Long processoId);
}
