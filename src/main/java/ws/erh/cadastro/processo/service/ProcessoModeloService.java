package ws.erh.cadastro.processo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ws.common.auth.TenantContext;
import ws.erh.cadastro.processo.repository.ProcessoModeloRepository;
import ws.erh.core.base.AbstractTenantService;
import ws.erh.core.enums.processo.CategoriaProcesso;
import ws.erh.core.exception.EntityNotFoundException;
import ws.erh.model.cadastro.processo.ProcessoCampoModelo;
import ws.erh.model.cadastro.processo.ProcessoDocumentoModelo;
import ws.erh.model.cadastro.processo.ProcessoEtapaModelo;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.util.List;

@Service
@Transactional
@Slf4j
public class ProcessoModeloService extends AbstractTenantService implements ProcessoModeloServiceInterface {

    @Autowired
    private ProcessoModeloRepository processoModeloRepository;

    @Override
    public ProcessoModelo saveProcessoModelo(ProcessoModelo modelo) {
        log.info("Salvando modelo de processo: {}", modelo.getCodigo());
        Long unidadeGestoraId = getCurrentUnidadeGestoraId();
        if (processoModeloRepository.existsByCodigoAndUnidadeGestoraId(modelo.getCodigo(), unidadeGestoraId)) {
            throw new IllegalStateException("Ja existe um modelo de processo com o codigo: " + modelo.getCodigo());
        }

        configurarDadosTenant(modelo);
        vincularFilhos(modelo);
        return processoModeloRepository.save(modelo);
    }

    @Override
    public ProcessoModelo updateProcessoModelo(ProcessoModelo modelo) {
        log.info("Atualizando modelo de processo ID: {}", modelo.getId());
        Long unidadeGestoraId = getCurrentUnidadeGestoraId();
        if (processoModeloRepository.existsByCodigoAndUnidadeGestoraIdAndIdNot(modelo.getCodigo(), unidadeGestoraId, modelo.getId())) {
            throw new IllegalStateException("Ja existe um modelo de processo com o codigo: " + modelo.getCodigo());
        }

        configurarDadosTenant(modelo);
        vincularFilhos(modelo);
        return processoModeloRepository.save(modelo);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessoModelo findById(Long id) {
        return processoModeloRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Modelo de processo nao encontrado com ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessoModelo findByCodigo(String codigo) {
        return processoModeloRepository.findByCodigoAndUnidadeGestoraId(codigo, getCurrentUnidadeGestoraId())
                .orElseThrow(() -> new EntityNotFoundException("Modelo de processo nao encontrado com codigo: " + codigo));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcessoModelo> getAll(Pageable pageable) {
        return processoModeloRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessoModelo> findAtivos() {
        return processoModeloRepository.findAtivos();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessoModelo> findVisivelPortal() {
        return processoModeloRepository.findVisivelPortal();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessoModelo> findByCategoria(CategoriaProcesso categoria) {
        return processoModeloRepository.findByCategoria(categoria);
    }

    @Override
    public void deleteProcessoModelo(Long id) {
        log.info("Excluindo modelo de processo ID: {}", id);
        ProcessoModelo modelo = findById(id);
        processoModeloRepository.delete(modelo);
    }

    private void vincularFilhos(ProcessoModelo modelo) {
        if (modelo.getEtapas() != null) {
            for (ProcessoEtapaModelo etapa : modelo.getEtapas()) {
                etapa.setProcessoModelo(modelo);
                configurarDadosTenant(etapa);
            }
        }
        if (modelo.getDocumentosExigidos() != null) {
            for (ProcessoDocumentoModelo doc : modelo.getDocumentosExigidos()) {
                doc.setProcessoModelo(modelo);
                if (doc.getEtapaModelo() != null) {
                    doc.getEtapaModelo().setProcessoModelo(modelo);
                }
                configurarDadosTenant(doc);
            }
        }
        if (modelo.getCamposAdicionais() != null) {
            for (ProcessoCampoModelo campo : modelo.getCamposAdicionais()) {
                campo.setProcessoModelo(modelo);
                if (campo.getEtapaModelo() != null) {
                    campo.getEtapaModelo().setProcessoModelo(modelo);
                }
                configurarDadosTenant(campo);
            }
        }
    }

    private Long getCurrentUnidadeGestoraId() {
        Long unidadeGestoraId = TenantContext.getCurrentUnidadeGestoraId();
        if (unidadeGestoraId == null) {
            throw new IllegalStateException("Unidade gestora nao encontrada no contexto da requisicao.");
        }
        return unidadeGestoraId;
    }
}
