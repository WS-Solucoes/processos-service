package ws.erh.cadastro.processo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ws.erh.core.enums.processo.CategoriaProcesso;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessoModeloRepository extends JpaRepository<ProcessoModelo, Long> {

    @Query("SELECT m FROM ProcessoModelo m WHERE m.codigo = :codigo AND m.excluido = false")
    Optional<ProcessoModelo> findByCodigo(@Param("codigo") String codigo);

    @Query("""
            SELECT m
            FROM ProcessoModelo m
            WHERE m.codigo = :codigo
              AND m.unidadeGestoraId = :unidadeGestoraId
              AND m.excluido = false
            """)
    Optional<ProcessoModelo> findByCodigoAndUnidadeGestoraId(@Param("codigo") String codigo,
                                                             @Param("unidadeGestoraId") Long unidadeGestoraId);

    @Query("SELECT m FROM ProcessoModelo m WHERE m.ativo = true AND m.excluido = false ORDER BY m.ordemExibicao, m.nome")
    List<ProcessoModelo> findAtivos();

    @Query("SELECT m FROM ProcessoModelo m WHERE m.ativo = true AND m.visivelPortal = true AND m.excluido = false ORDER BY m.ordemExibicao, m.nome")
    List<ProcessoModelo> findVisivelPortal();

    @Query("SELECT m FROM ProcessoModelo m WHERE m.categoria = :categoria AND m.ativo = true AND m.excluido = false ORDER BY m.nome")
    List<ProcessoModelo> findByCategoria(@Param("categoria") CategoriaProcesso categoria);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM ProcessoModelo m WHERE m.codigo = :codigo AND m.excluido = false")
    boolean existsByCodigo(@Param("codigo") String codigo);

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM ProcessoModelo m
            WHERE m.codigo = :codigo
              AND m.unidadeGestoraId = :unidadeGestoraId
              AND m.excluido = false
            """)
    boolean existsByCodigoAndUnidadeGestoraId(@Param("codigo") String codigo,
                                              @Param("unidadeGestoraId") Long unidadeGestoraId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM ProcessoModelo m WHERE m.codigo = :codigo AND m.id <> :id AND m.excluido = false")
    boolean existsByCodigoAndIdNot(@Param("codigo") String codigo, @Param("id") Long id);

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM ProcessoModelo m
            WHERE m.codigo = :codigo
              AND m.unidadeGestoraId = :unidadeGestoraId
              AND m.id <> :id
              AND m.excluido = false
            """)
    boolean existsByCodigoAndUnidadeGestoraIdAndIdNot(@Param("codigo") String codigo,
                                                      @Param("unidadeGestoraId") Long unidadeGestoraId,
                                                      @Param("id") Long id);
}
