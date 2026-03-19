package ws.processos.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessoOutboxEventRepository extends JpaRepository<ProcessoOutboxEvent, Long> {
    List<ProcessoOutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(ProcessoOutboxStatus status);
}
