package ws.erh.cadastro.processo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ws.erh.core.enums.processo.StatusComplementacaoProcesso;
import ws.erh.model.cadastro.processo.ProcessoComplementacao;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessoComplementacaoRepository extends JpaRepository<ProcessoComplementacao, Long> {

    @Query("""
            SELECT c FROM ProcessoComplementacao c
            WHERE c.processo.id = :processoId
              AND c.excluido = false
            ORDER BY c.dataSolicitacao DESC
            """)
    List<ProcessoComplementacao> findByProcessoId(@Param("processoId") Long processoId);

    @Query("""
            SELECT c FROM ProcessoComplementacao c
            WHERE c.processo.id = :processoId
              AND c.status = :status
              AND c.excluido = false
            ORDER BY c.dataSolicitacao DESC
            """)
    Optional<ProcessoComplementacao> findFirstByProcessoIdAndStatus(@Param("processoId") Long processoId,
                                                                    @Param("status") StatusComplementacaoProcesso status);
}
