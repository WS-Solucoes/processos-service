package ws.erh.cadastro.processo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ws.common.auth.TenantContext;
import ws.common.auth.UsuarioDetails;
import ws.common.auth.UsuarioDetailsService;
import ws.common.model.UnidadeGestora;
import ws.common.model.Usuario;
import ws.common.repository.UnidadeGestoraRepository;
import ws.erh.cadastro.processo.repository.ProcessoCampoModeloRepository;
import ws.erh.cadastro.processo.repository.ProcessoDocumentoModeloRepository;
import ws.erh.cadastro.processo.repository.ProcessoEtapaModeloRepository;
import ws.erh.cadastro.processo.repository.ProcessoModeloRepository;
import ws.erh.core.enums.processo.CategoriaProcesso;
import ws.erh.core.exception.EntityNotFoundException;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessoModeloService - Testes unitários")
class ProcessoModeloServiceTest {

    @InjectMocks
    private ProcessoModeloService service;

    @Mock
    private ProcessoModeloRepository processoModeloRepository;

    @Mock
    private ProcessoDocumentoModeloRepository documentoModeloRepository;

    @Mock
    private ProcessoEtapaModeloRepository etapaModeloRepository;

    @Mock
    private ProcessoCampoModeloRepository campoModeloRepository;

    @Mock
    private UnidadeGestoraRepository unidadeGestoraRepository;

    @Mock
    private UsuarioDetailsService usuarioDetailsService;

    private ProcessoModelo modelo;

    @BeforeEach
    void setUp() {
        // Configurar contexto de tenant
        TenantContext.setCurrentUnidadeGestoraId(1L);
        TenantContext.setCurrentUnidadeGestoraRole("ADMIN");
        UnidadeGestora ug = new UnidadeGestora();
        ug.setId(1L);
        lenient().when(unidadeGestoraRepository.findById(1L)).thenReturn(Optional.of(ug));
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        UsuarioDetails usuarioDetails = new UsuarioDetails(usuario);
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(usuarioDetails);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        modelo = new ProcessoModelo();
        modelo.setCodigo("FERIAS");
        modelo.setNome("Solicitação de Férias");
        modelo.setDescricao("Modelo para solicitação de férias");
        modelo.setCategoria(CategoriaProcesso.FERIAS);
        modelo.setPrazoAtendimentoDias(30);
        modelo.setAtivo(true);
        modelo.setVisivelPortal(true);
        modelo.setDocumentosExigidos(new ArrayList<>());
        modelo.setEtapas(new ArrayList<>());
        modelo.setCamposAdicionais(new ArrayList<>());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Criação de modelo")
    class CriacaoModelo {

        @Test
        @DisplayName("Deve criar modelo com sucesso")
        void deveCriarModeloComSucesso() {
            when(processoModeloRepository.existsByCodigo("FERIAS")).thenReturn(false);
            when(processoModeloRepository.save(any())).thenAnswer(inv -> {
                ProcessoModelo m = inv.getArgument(0);
                m.setId(1L);
                return m;
            });

            ProcessoModelo result = service.saveProcessoModelo(modelo);

            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("FERIAS", result.getCodigo());
            verify(processoModeloRepository).save(any());
        }

        @Test
        @DisplayName("Deve rejeitar código duplicado")
        void deveRejeitarCodigoDuplicado() {
            when(processoModeloRepository.existsByCodigo("FERIAS")).thenReturn(true);

            assertThrows(IllegalStateException.class, () -> service.saveProcessoModelo(modelo));
            verify(processoModeloRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Atualização de modelo")
    class AtualizacaoModelo {

        @Test
        @DisplayName("Deve atualizar modelo existente")
        void deveAtualizarModelo() {
            modelo.setId(1L);
            modelo.setNome("Solicitação de Férias Atualizada");
            when(processoModeloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProcessoModelo result = service.updateProcessoModelo(modelo);

            assertNotNull(result);
            assertEquals("Solicitação de Férias Atualizada", result.getNome());
        }
    }

    @Nested
    @DisplayName("Busca de modelo")
    class BuscaModelo {

        @Test
        @DisplayName("Deve retornar modelo por ID")
        void deveRetornarPorId() {
            modelo.setId(1L);
            when(processoModeloRepository.findById(1L)).thenReturn(Optional.of(modelo));

            ProcessoModelo result = service.findById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Deve lançar exceção quando modelo não encontrado por ID")
        void deveLancarExcecaoNaoEncontradoPorId() {
            when(processoModeloRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.findById(99L));
        }

        @Test
        @DisplayName("Deve retornar modelo por código")
        void deveRetornarPorCodigo() {
            modelo.setId(1L);
            when(processoModeloRepository.findByCodigo("FERIAS")).thenReturn(Optional.of(modelo));

            ProcessoModelo result = service.findByCodigo("FERIAS");

            assertNotNull(result);
            assertEquals("FERIAS", result.getCodigo());
        }

        @Test
        @DisplayName("Deve lançar exceção quando modelo não encontrado por código")
        void deveLancarExcecaoNaoEncontradoPorCodigo() {
            when(processoModeloRepository.findByCodigo("XPTO")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.findByCodigo("XPTO"));
        }

        @Test
        @DisplayName("Deve retornar lista de modelos ativos")
        void deveRetornarAtivos() {
            modelo.setId(1L);
            when(processoModeloRepository.findAtivos()).thenReturn(List.of(modelo));

            List<ProcessoModelo> result = service.findAtivos();

            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Deve retornar modelos visíveis no portal")
        void deveRetornarVisivelPortal() {
            modelo.setId(1L);
            when(processoModeloRepository.findVisivelPortal()).thenReturn(List.of(modelo));

            List<ProcessoModelo> result = service.findVisivelPortal();

            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Deve retornar modelos por categoria")
        void deveRetornarPorCategoria() {
            modelo.setId(1L);
            when(processoModeloRepository.findByCategoria(CategoriaProcesso.FERIAS))
                    .thenReturn(List.of(modelo));

            List<ProcessoModelo> result = service.findByCategoria(CategoriaProcesso.FERIAS);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("Exclusão de modelo")
    class ExclusaoModelo {

        @Test
        @DisplayName("Deve excluir modelo existente")
        void deveExcluirModelo() {
            modelo.setId(1L);
            when(processoModeloRepository.findById(1L)).thenReturn(Optional.of(modelo));

            service.deleteProcessoModelo(1L);

            verify(processoModeloRepository).delete(modelo);
        }

        @Test
        @DisplayName("Deve lançar exceção ao excluir modelo inexistente")
        void deveLancarExcecaoAoExcluirInexistente() {
            when(processoModeloRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.deleteProcessoModelo(99L));
        }
    }
}
