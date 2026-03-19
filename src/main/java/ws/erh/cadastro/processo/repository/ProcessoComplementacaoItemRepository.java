package ws.erh.cadastro.processo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ws.erh.model.cadastro.processo.ProcessoComplementacaoItem;

@Repository
public interface ProcessoComplementacaoItemRepository extends JpaRepository<ProcessoComplementacaoItem, Long> {
}
