package ws.erh.cadastro.processo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ws.erh.model.cadastro.processo.ConfiguracaoAutoArquivamento;

@Repository
public interface ConfiguracaoAutoArquivamentoRepository extends JpaRepository<ConfiguracaoAutoArquivamento, Long> {
}
