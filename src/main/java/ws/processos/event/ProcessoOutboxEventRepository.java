package ws.processos.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProcessoOutboxEventRepository extends JpaRepository<ProcessoOutboxEvent, Long> {

    List<ProcessoOutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(ProcessoOutboxStatus status);

    long countByStatus(ProcessoOutboxStatus status);

    /** Idade do registro mais antigo em determinado status (para detectar SLA estourado). */
    @Query("SELECT MIN(e.createdAt) FROM ProcessoOutboxEvent e WHERE e.status = :status")
    Optional<LocalDateTime> findOldestCreatedAtByStatus(@Param("status") ProcessoOutboxStatus status);

    /** Lista os erros mais recentes (paginado). */
    List<ProcessoOutboxEvent> findByStatusOrderByCreatedAtDesc(ProcessoOutboxStatus status, Pageable pageable);

    /** Erros agrupados por aggregateType + eventType para o resumo do dashboard. */
    @Query("SELECT e.aggregateType, e.eventType, COUNT(e) "
            + "FROM ProcessoOutboxEvent e "
            + "WHERE e.status = :status "
            + "GROUP BY e.aggregateType, e.eventType "
            + "ORDER BY COUNT(e) DESC")
    List<Object[]> contarErrosPorTipo(@Param("status") ProcessoOutboxStatus status);
}
