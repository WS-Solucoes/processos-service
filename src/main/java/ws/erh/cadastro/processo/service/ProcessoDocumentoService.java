package ws.erh.cadastro.processo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ws.erh.cadastro.processo.repository.ProcessoDocumentoModeloRepository;
import ws.erh.cadastro.processo.repository.ProcessoDocumentoRepository;
import ws.erh.core.base.AbstractTenantService;
import ws.erh.core.enums.processo.SituacaoDocumento;
import ws.erh.core.exception.EntityNotFoundException;
import ws.erh.core.storage.ArquivoStorageService;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoDocumento;
import ws.erh.model.cadastro.processo.ProcessoDocumentoModelo;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@Slf4j
public class ProcessoDocumentoService extends AbstractTenantService implements ProcessoDocumentoServiceInterface {

    @Autowired
    private ProcessoDocumentoRepository documentoRepository;

    @Autowired
    private ArquivoStorageService arquivoStorageService;

    @Autowired
    private ProcessoStoragePathResolver storagePathResolver;

    @Autowired
    private ProcessoDocumentoModeloRepository documentoModeloRepository;

    @Override
    public ProcessoDocumento enviarDocumento(ProcessoDocumento documento) {
        log.info("Enviando documento para processo ID: {}", documento.getProcesso().getId());
        documento.setSituacao(SituacaoDocumento.PENDENTE);
        documento.setDataEnvio(LocalDateTime.now());
        configurarDadosTenant(documento);
        return documentoRepository.save(documento);
    }

    @Override
    public ProcessoDocumento uploadDocumento(Processo processo, MultipartFile arquivo, Long documentoModeloId, String enviadoPor) {
        try {
            ProcessoDocumentoModelo documentoModelo = documentoModeloId != null
                    ? documentoModeloRepository.findById(documentoModeloId).orElseThrow(
                            () -> new IllegalStateException("Documento modelo não encontrado: " + documentoModeloId))
                    : null;

            String caminho = storagePathResolver.resolve(processo, documentoModelo, arquivo.getOriginalFilename());
            arquivoStorageService.save(caminho, arquivo.getBytes());

            ProcessoDocumento documento = new ProcessoDocumento();
            documento.setProcesso(processo);
            documento.setDocumentoModelo(documentoModelo);
            documento.setNomeArquivo(arquivo.getOriginalFilename());
            documento.setCaminhoStorage(caminho);
            documento.setTipoArquivo(arquivo.getContentType());
            documento.setTamanhoBytes(arquivo.getSize());
            documento.setEnviadoPor(enviadoPor);

            return enviarDocumento(documento);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar documento: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessoDocumento findById(Long id) {
        return documentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Documento não encontrado com ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessoDocumento> findByProcessoId(Long processoId) {
        return documentoRepository.findByProcessoId(processoId);
    }

    @Override
    public ProcessoDocumento aceitarDocumento(Long documentoId, String avaliadoPor) {
        log.info("Aceitando documento ID: {}", documentoId);
        ProcessoDocumento doc = findById(documentoId);
        doc.setSituacao(SituacaoDocumento.ACEITO);
        doc.setAvaliadoPor(avaliadoPor);
        doc.setDataAvaliacao(LocalDateTime.now());
        configurarDadosTenant(doc);
        return documentoRepository.save(doc);
    }

    @Override
    public ProcessoDocumento recusarDocumento(Long documentoId, String motivoRecusa, String avaliadoPor) {
        log.info("Recusando documento ID: {}", documentoId);
        ProcessoDocumento doc = findById(documentoId);
        doc.setSituacao(SituacaoDocumento.RECUSADO);
        doc.setMotivoRecusa(motivoRecusa);
        doc.setAvaliadoPor(avaliadoPor);
        doc.setDataAvaliacao(LocalDateTime.now());
        configurarDadosTenant(doc);
        return documentoRepository.save(doc);
    }

    @Override
    public void deleteDocumento(Long id) {
        log.info("Excluindo documento ID: {}", id);
        ProcessoDocumento doc = findById(id);
        documentoRepository.delete(doc);
    }

    @Override
    public void deleteDocumentoWithStorage(Long id) {
        ProcessoDocumento doc = findById(id);
        try {
            arquivoStorageService.delete(doc.getCaminhoStorage());
        } catch (Exception e) {
            log.warn("Falha ao excluir arquivo do storage para documento {}: {}", id, e.getMessage());
        }
        documentoRepository.delete(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadDocumento(Long id) {
        ProcessoDocumento doc = findById(id);
        try {
            return arquivoStorageService.load(doc.getCaminhoStorage());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao baixar documento: " + e.getMessage(), e);
        }
    }
}
