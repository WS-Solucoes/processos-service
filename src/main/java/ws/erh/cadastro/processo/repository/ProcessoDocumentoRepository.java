package ws.erh.cadastro.processo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ws.erh.core.enums.processo.SituacaoDocumento;
import ws.erh.model.cadastro.processo.ProcessoDocumento;

import java.util.List;

@Repository
public interface ProcessoDocumentoRepository extends JpaRepository<ProcessoDocumento, Long> {

    @Query("SELECT d FROM ProcessoDocumento d WHERE d.processo.id = :processoId AND d.excluido = false ORDER BY d.dataEnvio")
    List<ProcessoDocumento> findByProcessoId(@Param("processoId") Long processoId);

    @Query("SELECT d FROM ProcessoDocumento d WHERE d.processo.id = :processoId AND d.situacao = :situacao AND d.excluido = false")
    List<ProcessoDocumento> findByProcessoIdAndSituacao(
            @Param("processoId") Long processoId,
            @Param("situacao") SituacaoDocumento situacao);

    @Query("SELECT COUNT(d) FROM ProcessoDocumento d WHERE d.processo.id = :processoId AND d.excluido = false")
    long countByProcessoId(@Param("processoId") Long processoId);

    @Query("SELECT COUNT(d) FROM ProcessoDocumento d WHERE d.processo.id = :processoId AND d.situacao = :situacao AND d.excluido = false")
    long countByProcessoIdAndSituacao(@Param("processoId") Long processoId, @Param("situacao") SituacaoDocumento situacao);

    default long countPendentesByProcessoId(Long processoId) {
        return countByProcessoIdAndSituacao(processoId, SituacaoDocumento.PENDENTE);
    }
}
