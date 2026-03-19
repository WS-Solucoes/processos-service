package ws.erh.cadastro.processo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.model.cadastro.processo.ProcessoModelo;
import ws.erh.model.cadastro.processo.Processo;

import java.util.List;
import java.util.Map;

public interface ProcessoServiceInterface {

    Processo abrirProcesso(Processo processo);
    Processo solicitarProcesso(Processo processo, String usuario, String descricaoInicial);
    Processo findById(Long id);
    Processo findByIdAndServidorId(Long id, Long servidorId);
    Processo findByProtocolo(String protocolo);
    Page<Processo> getAll(Pageable pageable);
    Page<Processo> findByServidorId(Long servidorId, Pageable pageable);
    List<Processo> findBySituacao(SituacaoProcesso situacao);
    List<Processo> findVencidos();
    Page<Processo> findPendentes(Pageable pageable);
    Processo cancelar(Long id, String justificativa, String usuario);
    void deleteProcesso(Long id);
    Map<String, Object> getDashboard();
    String gerarProtocolo();
}
