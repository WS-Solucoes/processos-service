package ws.erh.cadastro.processo.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ws.common.model.Endereco;
import ws.common.model.Municipio;
import ws.common.model.UnidadeGestora;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoDocumentoModelo;
import ws.erh.model.cadastro.servidor.Servidor;

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

        assertTrue(path.startsWith("ws-service/sao-jose__7/fundo-municipal-de-saude__11/jose-da-silva__19/comprovante-de-ferias__9/PROC-2026-000001/uploads/"));
        assertTrue(path.endsWith(".pdf"));
        assertTrue(path.contains("__meu-arquivo.pdf"));
    }

    @Test
    @DisplayName("Deve usar municipio do servidor como origem principal")
    void deveUsarMunicipioDoServidor() {
        Processo processo = buildProcesso();
        String municipioSegment = resolver.resolveMunicipioSegment(processo);
        assertEquals("sao-jose__7", municipioSegment);
    }

    @Test
    @DisplayName("Deve usar municipio da unidade gestora quando servidor nao possuir municipio")
    void deveUsarMunicipioDaUnidadeGestoraQuandoServidorNaoPossuirMunicipio() {
        Processo processo = buildProcesso();
        processo.getServidor().setMunicipio(null);

        String municipioSegment = resolver.resolveMunicipioSegment(processo);

        assertEquals("fortaleza__11", municipioSegment);
    }

    @Test
    @DisplayName("Deve usar fallback quando nao houver municipio")
    void deveUsarFallbackQuandoNaoHouverMunicipio() {
        Processo processo = buildProcesso();
        processo.getServidor().setMunicipio(null);
        processo.getUnidadeGestora().getEndereco().setMunicipio(null);

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
        Municipio municipio = new Municipio();
        municipio.setId(7L);
        municipio.setNome("Sao Jose");

        Servidor servidor = new Servidor();
        servidor.setId(19L);
        servidor.setNome("Jose da Silva");
        servidor.setMunicipio(municipio);

        Endereco endereco = new Endereco();
        endereco.setMunicipio("Fortaleza");

        UnidadeGestora unidadeGestora = new UnidadeGestora();
        unidadeGestora.setId(11L);
        unidadeGestora.setNome("Fundo Municipal de Saude");
        unidadeGestora.setEndereco(endereco);

        Processo processo = new Processo();
        processo.setProtocolo("PROC-2026-000001");
        processo.setServidor(servidor);
        processo.setUnidadeGestora(unidadeGestora);
        return processo;
    }
}
