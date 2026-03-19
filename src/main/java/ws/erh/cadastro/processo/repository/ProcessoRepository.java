package ws.erh.cadastro.processo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.model.cadastro.processo.Processo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessoRepository extends JpaRepository<Processo, Long> {

    @Query("SELECT p FROM Processo p WHERE p.protocolo = :protocolo AND p.excluido = false")
    Optional<Processo> findByProtocolo(@Param("protocolo") String protocolo);

    @Query("SELECT p FROM Processo p WHERE p.id = :id AND p.servidorId = :servidorId AND p.excluido = false")
    Optional<Processo> findByIdAndServidorId(@Param("id") Long id, @Param("servidorId") Long servidorId);

    @Query("SELECT p FROM Processo p WHERE p.servidorId = :servidorId AND p.excluido = false ORDER BY p.dataAbertura DESC")
    List<Processo> findByServidorId(@Param("servidorId") Long servidorId);

    @Query("SELECT p FROM Processo p WHERE p.servidorId = :servidorId AND p.excluido = false ORDER BY p.dataAbertura DESC")
    Page<Processo> findByServidorId(@Param("servidorId") Long servidorId, Pageable pageable);

    @Query("""
            SELECT COUNT(p) FROM Processo p
            WHERE p.servidorId = :servidorId
              AND p.unidadeGestoraId = :unidadeGestoraId
              AND p.situacao NOT IN :situacoesFinalizadas
              AND p.excluido = false
            """)
    long countProcessosAbertos(@Param("servidorId") Long servidorId,
                               @Param("unidadeGestoraId") Long unidadeGestoraId,
                               @Param("situacoesFinalizadas") List<SituacaoProcesso> situacoesFinalizadas);

    @Query("SELECT p FROM Processo p WHERE p.situacao = :situacao AND p.excluido = false ORDER BY p.dataAbertura DESC")
    List<Processo> findBySituacao(@Param("situacao") SituacaoProcesso situacao);

    @Query("SELECT p FROM Processo p WHERE p.situacao IN :situacoes AND p.excluido = false ORDER BY p.prioridade, p.dataAbertura")
    Page<Processo> findBySituacaoIn(@Param("situacoes") List<SituacaoProcesso> situacoes, Pageable pageable);

    @Query("SELECT p FROM Processo p WHERE p.atribuidoPara = :usuario AND p.excluido = false ORDER BY p.prioridade, p.dataAbertura")
    Page<Processo> findByAtribuidoPara(@Param("usuario") String usuario, Pageable pageable);

    @Query("SELECT p FROM Processo p WHERE p.processoModelo.id = :modeloId AND p.excluido = false ORDER BY p.dataAbertura DESC")
    List<Processo> findByProcessoModeloId(@Param("modeloId") Long modeloId);

    @Query("SELECT COUNT(p) FROM Processo p WHERE p.situacao = :situacao AND p.excluido = false")
    long countBySituacao(@Param("situacao") SituacaoProcesso situacao);

    @Query("SELECT COUNT(p) FROM Processo p WHERE p.dataAbertura BETWEEN :inicio AND :fim AND p.excluido = false")
    long countByPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT p FROM Processo p WHERE p.prazoLimite < CURRENT_DATE AND p.situacao NOT IN :exclusoes AND p.excluido = false")
    List<Processo> findVencidosExcluindo(@Param("exclusoes") List<SituacaoProcesso> exclusoes);

    default List<Processo> findVencidos() {
        return findVencidosExcluindo(java.util.Arrays.asList(
                SituacaoProcesso.CONCLUIDO,
                SituacaoProcesso.CANCELADO,
                SituacaoProcesso.ARQUIVADO));
    }

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.protocolo, 11) AS int)), 0) FROM Processo p WHERE p.protocolo LIKE :prefixo%")
    int findMaxSequencialByAno(@Param("prefixo") String prefixo);
}
