package ws.erh.cadastro.processo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ws.erh.model.cadastro.processo.ProcessoCampoModelo;

import java.util.List;

@Repository
public interface ProcessoCampoModeloRepository extends JpaRepository<ProcessoCampoModelo, Long> {

    @Query("SELECT c FROM ProcessoCampoModelo c WHERE c.processoModelo.id = :modeloId AND c.excluido = false ORDER BY c.ordem")
    List<ProcessoCampoModelo> findByProcessoModeloId(@Param("modeloId") Long modeloId);
}
