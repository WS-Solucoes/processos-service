package ws.erh.cadastro.processo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ws.erh.model.cadastro.processo.ProcessoMensagem;

import java.util.List;

@Repository
public interface ProcessoMensagemRepository extends JpaRepository<ProcessoMensagem, Long> {

    @Query("SELECT m FROM ProcessoMensagem m WHERE m.processo.id = :processoId AND m.excluido = false ORDER BY m.dataHora ASC")
    List<ProcessoMensagem> findByProcessoId(@Param("processoId") Long processoId);

    @Query("SELECT COUNT(m) FROM ProcessoMensagem m WHERE m.processo.id = :processoId AND (m.lida = false OR m.lida IS NULL) AND m.excluido = false")
    long countNaoLidasByProcessoId(@Param("processoId") Long processoId);

    @Modifying
    @Query("UPDATE ProcessoMensagem m SET m.lida = true, m.dataLeitura = CURRENT_TIMESTAMP WHERE m.processo.id = :processoId AND (m.lida = false OR m.lida IS NULL)")
    void marcarTodasComoLidas(@Param("processoId") Long processoId);
}
