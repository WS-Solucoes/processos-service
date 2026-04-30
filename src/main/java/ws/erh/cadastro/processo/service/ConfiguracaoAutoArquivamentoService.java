package ws.erh.cadastro.processo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ws.erh.cadastro.processo.repository.ConfiguracaoAutoArquivamentoRepository;
import ws.erh.core.enums.processo.CategoriaProcesso;
import ws.erh.model.cadastro.processo.ConfiguracaoAutoArquivamento;

/**
 * Serviço de configuração de auto-arquivamento (S7.10).
 *
 * <p>Singleton (id=1). Cria registro padrão na primeira leitura caso ausente.</p>
 */
@Service
public class ConfiguracaoAutoArquivamentoService {

    private static final Long SINGLETON_ID = 1L;

    @Autowired
    private ConfiguracaoAutoArquivamentoRepository repository;

    @Transactional
    public ConfiguracaoAutoArquivamento obterOuCriarPadrao() {
        return repository.findById(SINGLETON_ID).orElseGet(() -> {
            ConfiguracaoAutoArquivamento c = new ConfiguracaoAutoArquivamento();
            c.setId(SINGLETON_ID);
            return repository.save(c);
        });
    }

    @Transactional
    public ConfiguracaoAutoArquivamento atualizar(ConfiguracaoAutoArquivamento atualizada) {
        ConfiguracaoAutoArquivamento existente = obterOuCriarPadrao();
        existente.setHabilitado(atualizada.getHabilitado() != null ? atualizada.getHabilitado() : true);
        existente.setDiasConcluido(nz(atualizada.getDiasConcluido(), 90));
        existente.setDiasCancelado(nz(atualizada.getDiasCancelado(), 90));
        existente.setDiasPendenteDocumentacao(nz(atualizada.getDiasPendenteDocumentacao(), 30));
        existente.setSlaDiasPadrao(nz(atualizada.getSlaDiasPadrao(), 60));
        existente.setSlaFerias(nz(atualizada.getSlaFerias(), 30));
        existente.setSlaAfastamento(nz(atualizada.getSlaAfastamento(), 45));
        existente.setSlaLicenca(nz(atualizada.getSlaLicenca(), 45));
        existente.setSlaRescisao(nz(atualizada.getSlaRescisao(), 30));
        existente.setSlaCadastral(nz(atualizada.getSlaCadastral(), 60));
        existente.setSlaFinanceiro(nz(atualizada.getSlaFinanceiro(), 90));
        existente.setSlaDocumental(nz(atualizada.getSlaDocumental(), 60));
        existente.setSlaOutros(nz(atualizada.getSlaOutros(), 60));
        return repository.save(existente);
    }

    public int slaDiasPara(CategoriaProcesso categoria) {
        ConfiguracaoAutoArquivamento c = obterOuCriarPadrao();
        if (categoria == null) return c.getSlaDiasPadrao();
        return switch (categoria) {
            case FERIAS -> c.getSlaFerias();
            case AFASTAMENTO -> c.getSlaAfastamento();
            case LICENCA -> c.getSlaLicenca();
            case RESCISAO -> c.getSlaRescisao();
            case CADASTRAL -> c.getSlaCadastral();
            case FINANCEIRO -> c.getSlaFinanceiro();
            case DOCUMENTAL -> c.getSlaDocumental();
            case OUTROS, LIBERACAO_LOTE_PAGAMENTO -> c.getSlaOutros();
        };
    }

    private static int nz(Integer v, int def) {
        return v == null ? def : v;
    }
}
