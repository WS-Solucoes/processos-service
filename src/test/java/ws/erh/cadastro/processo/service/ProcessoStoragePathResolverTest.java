package ws.erh.cadastro.processo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoDocumentoModelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProcessoStoragePathResolver - Testes unitarios")
class ProcessoStoragePathResolverTest {

    private final ProcessoStoragePathResolver resolver = new ProcessoStoragePathResolver();

    @Test
    @DisplayName("Deve montar path multi-tenant com slug e id")
    void deveMontarPathMultiTenantComSlugEId() {
        Processo processo = buildProcesso();
        ProcessoDocumentoModelo documentoModelo = new ProcessoDocumentoModelo();
        documentoModelo.setId(9L);
        documentoModelo.setNome("Comprovante de Ferias");

        String path = resolver.resolve(processo, documentoModelo, "Meu Arquivo.PDF");

        assertTrue(path.startsWith("ws-service/sao-jose__11/fundo-municipal-de-saude__11/jose-da-silva__19/comprovante-de-ferias__9/PROC-2026-000001/uploads/"));
        assertTrue(path.endsWith(".pdf"));
        assertTrue(path.contains("__meu-arquivo.pdf"));
    }

    @Test
    @DisplayName("Deve usar municipio do servidor como origem principal")
    void deveUsarMunicipioDoServidor() {
        Processo processo = buildProcesso();
        String municipioSegment = resolver.resolveMunicipioSegment(processo);
        assertEquals("sao-jose__11", municipioSegment);
    }

    @Test
    @DisplayName("Deve usar fallback quando municipio snapshot estiver vazio")
    void deveUsarFallbackQuandoMunicipioSnapshotEstiverVazio() {
        Processo processo = buildProcesso();
        processo.setMunicipioNome(" ");

        String municipioSegment = resolver.resolveMunicipioSegment(processo);

        assertEquals("sem-municipio__0", municipioSegment);
    }

    @Test
    @DisplayName("Deve usar fallback quando nao houver municipio")
    void deveUsarFallbackQuandoNaoHouverMunicipio() {
        Processo processo = buildProcesso();
        processo.setMunicipioNome(null);

        String municipioSegment = resolver.resolveMunicipioSegment(processo);

        assertEquals("sem-municipio__0", municipioSegment);
    }

    @Test
    @DisplayName("Deve usar anexo geral quando documento modelo nao for informado")
    void deveUsarAnexoGeralQuandoDocumentoModeloNaoForInformado() {
        Processo processo = buildProcesso();

        String path = resolver.resolve(processo, null, "Documento sem tipo.docx");

        assertTrue(path.contains("/anexo-geral__0/"));
        assertTrue(path.endsWith(".docx"));
    }

    private Processo buildProcesso() {
        Processo processo = new Processo();
        processo.setProtocolo("PROC-2026-000001");
        processo.setServidorId(19L);
        processo.setServidorNome("Jose da Silva");
        processo.setUnidadeGestoraId(11L);
        processo.setUnidadeGestoraNome("Fundo Municipal de Saude");
        processo.setMunicipioNome("Sao Jose");
        return processo;
    }
}
