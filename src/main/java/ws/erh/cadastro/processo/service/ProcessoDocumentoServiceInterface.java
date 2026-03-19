package ws.erh.cadastro.processo.service;

import org.springframework.web.multipart.MultipartFile;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoDocumento;

import java.util.List;

public interface ProcessoDocumentoServiceInterface {

    ProcessoDocumento enviarDocumento(ProcessoDocumento documento);
    ProcessoDocumento uploadDocumento(Processo processo, MultipartFile arquivo, Long documentoModeloId, String enviadoPor);
    ProcessoDocumento findById(Long id);
    List<ProcessoDocumento> findByProcessoId(Long processoId);
    ProcessoDocumento aceitarDocumento(Long documentoId, String avaliadoPor);
    ProcessoDocumento recusarDocumento(Long documentoId, String motivoRecusa, String avaliadoPor);
    void deleteDocumento(Long id);
    void deleteDocumentoWithStorage(Long id);
    byte[] downloadDocumento(Long id);
}
