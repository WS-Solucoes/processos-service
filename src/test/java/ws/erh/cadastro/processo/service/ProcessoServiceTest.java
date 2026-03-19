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
import ws.erh.cadastro.processo.repository.ProcessoHistoricoRepository;
import ws.erh.cadastro.processo.repository.ProcessoRepository;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.core.exception.EntityNotFoundException;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoModelo;
import ws.erh.model.cadastro.servidor.Servidor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessoService - Testes unitários")
class ProcessoServiceTest {

    @InjectMocks
    private ProcessoService service;

    @Mock
    private ProcessoRepository processoRepository;

    @Mock
    private ProcessoHistoricoRepository historicoRepository;

    @Mock
    private ProcessoModeloServiceInterface processoModeloService;

    @Mock
    private ProcessoWorkflowService workflowService;

    @Mock
    private UnidadeGestoraRepository unidadeGestoraRepository;

    @Mock
    private UsuarioDetailsService usuarioDetailsService;

    private Processo processo;
    private ProcessoModelo modelo;
    private Servidor servidor;

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

        servidor = new Servidor();
        servidor.setId(1L);

        modelo = new ProcessoModelo();
        modelo.setId(1L);
        modelo.setCodigo("FERIAS");
        modelo.setNome("Solicitação de Férias");
        modelo.setPrazoAtendimentoDias(30);

        processo = new Processo();
        processo.setServidor(servidor);
        processo.setProcessoModelo(modelo);

        lenient().doAnswer(invocation -> {
            Processo entity = invocation.getArgument(0);
            entity.setEtapaAtual(1);
            entity.setSituacao(SituacaoProcesso.ABERTO);
            return null;
        }).when(workflowService).prepararParaSubmissaoServidor(any(Processo.class));

        lenient().doAnswer(invocation -> {
            Processo entity = invocation.getArgument(0);
            entity.setEtapaAtual(1);
            entity.setSituacao(SituacaoProcesso.PENDENTE_DOCUMENTACAO);
            return null;
        }).when(workflowService).prepararParaSolicitacaoRh(any(Processo.class));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════════
    // ABERTURA DE PROCESSO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Abertura de processo")
    class AberturaProcesso {

        @Test
        @DisplayName("Deve abrir processo com protocolo e situação ABERTO")
        void deveAbrirProcessoComProtocolo() {
            when(processoRepository.findMaxSequencialByAno(any())).thenReturn(0);
            when(processoRepository.save(any())).thenAnswer(inv -> {
                Processo p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Processo result = service.abrirProcesso(processo);

            assertNotNull(result);
            assertEquals(SituacaoProcesso.ABERTO, result.getSituacao());
            assertNotNull(result.getProtocolo());
            assertTrue(result.getProtocolo().startsWith("PROC-"));
            assertEquals(1, result.getEtapaAtual());
            assertNotNull(result.getDataAbertura());
        }

        @Test
        @DisplayName("Deve calcular prazo limite com base no modelo")
        void deveCalcularPrazoLimite() {
            when(processoRepository.findMaxSequencialByAno(any())).thenReturn(0);
            when(processoRepository.save(any())).thenAnswer(inv -> {
                Processo p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Processo result = service.abrirProcesso(processo);

            assertNotNull(result.getPrazoLimite());
            assertEquals(LocalDate.now().plusDays(30), result.getPrazoLimite());
        }

        @Test
        @DisplayName("Deve registrar histórico na abertura")
        void deveRegistrarHistoricoNaAbertura() {
            when(processoRepository.findMaxSequencialByAno(any())).thenReturn(0);
            when(processoRepository.save(any())).thenAnswer(inv -> {
                Processo p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.abrirProcesso(processo);

            verify(historicoRepository).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BUSCA DE PROCESSOS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Busca de processos")
    class BuscaProcessos {

        @Test
        @DisplayName("Deve retornar processo por ID")
        void deveRetornarPorId() {
            processo.setId(1L);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

            Processo result = service.findById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Deve lançar exceção quando processo não encontrado por ID")
        void deveLancarExcecaoNaoEncontradoPorId() {
            when(processoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.findById(99L));
        }

        @Test
        @DisplayName("Deve retornar processo por protocolo")
        void deveRetornarPorProtocolo() {
            processo.setId(1L);
            processo.setProtocolo("PROC-2025-000001");
            when(processoRepository.findByProtocolo("PROC-2025-000001")).thenReturn(Optional.of(processo));

            Processo result = service.findByProtocolo("PROC-2025-000001");

            assertNotNull(result);
            assertEquals("PROC-2025-000001", result.getProtocolo());
        }

        @Test
        @DisplayName("Deve lançar exceção quando protocolo não encontrado")
        void deveLancarExcecaoProtocoloNaoEncontrado() {
            when(processoRepository.findByProtocolo("INVALIDO")).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.findByProtocolo("INVALIDO"));
        }

        @Test
        @DisplayName("Deve retornar processos por situação")
        void deveRetornarPorSituacao() {
            processo.setId(1L);
            processo.setSituacao(SituacaoProcesso.ABERTO);
            when(processoRepository.findBySituacao(SituacaoProcesso.ABERTO)).thenReturn(List.of(processo));

            List<Processo> result = service.findBySituacao(SituacaoProcesso.ABERTO);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CANCELAMENTO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cancelamento de processo")
    class CancelamentoProcesso {

        @Test
        @DisplayName("Deve cancelar processo aberto")
        void deveCancelarProcessoAberto() {
            processo.setId(1L);
            processo.setSituacao(SituacaoProcesso.ABERTO);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
            when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Processo result = service.cancelar(1L, "Servidor desistiu", "admin");

            assertEquals(SituacaoProcesso.CANCELADO, result.getSituacao());
            assertNotNull(result.getDataConclusao());
        }

        @Test
        @DisplayName("Deve rejeitar cancelamento de processo concluído")
        void deveRejeitarCancelamentoConcluido() {
            processo.setId(1L);
            processo.setSituacao(SituacaoProcesso.CONCLUIDO);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

            assertThrows(IllegalStateException.class, () -> service.cancelar(1L, "Motivo", "admin"));
        }

        @Test
        @DisplayName("Deve rejeitar cancelamento de processo já cancelado")
        void deveRejeitarCancelamentoJaCancelado() {
            processo.setId(1L);
            processo.setSituacao(SituacaoProcesso.CANCELADO);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

            assertThrows(IllegalStateException.class, () -> service.cancelar(1L, "Motivo", "admin"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // EXCLUSÃO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Exclusão de processo")
    class ExclusaoProcesso {

        @Test
        @DisplayName("Deve excluir processo em rascunho")
        void deveExcluirRascunho() {
            processo.setId(1L);
            processo.setSituacao(SituacaoProcesso.RASCUNHO);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

            service.deleteProcesso(1L);

            verify(processoRepository).delete(processo);
        }

        @Test
        @DisplayName("Deve rejeitar exclusão de processo aberto")
        void deveRejeitarExclusaoAberto() {
            processo.setId(1L);
            processo.setSituacao(SituacaoProcesso.ABERTO);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

            assertThrows(IllegalStateException.class, () -> service.deleteProcesso(1L));
            verify(processoRepository, never()).delete(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DASHBOARD
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dashboard")
    class Dashboard {

        @Test
        @DisplayName("Deve retornar dashboard com todos os contadores")
        void deveRetornarDashboard() {
            when(processoRepository.countBySituacao(SituacaoProcesso.ABERTO)).thenReturn(5L);
            when(processoRepository.countBySituacao(SituacaoProcesso.EM_ANALISE)).thenReturn(3L);
            when(processoRepository.countBySituacao(SituacaoProcesso.PENDENTE_DOCUMENTACAO)).thenReturn(2L);
            when(processoRepository.countBySituacao(SituacaoProcesso.AGUARDANDO_CHEFIA)).thenReturn(1L);
            when(processoRepository.countBySituacao(SituacaoProcesso.DEFERIDO)).thenReturn(10L);
            when(processoRepository.countBySituacao(SituacaoProcesso.INDEFERIDO)).thenReturn(2L);
            when(processoRepository.countBySituacao(SituacaoProcesso.CONCLUIDO)).thenReturn(20L);
            when(processoRepository.countBySituacao(SituacaoProcesso.CANCELADO)).thenReturn(3L);
            when(processoRepository.findVencidos()).thenReturn(List.of(processo));
            when(processoRepository.countByPeriodo(any(), any())).thenReturn(8L);

            Map<String, Object> dashboard = service.getDashboard();

            assertNotNull(dashboard);
            assertEquals(5L, dashboard.get("abertos"));
            assertEquals(3L, dashboard.get("emAnalise"));
            assertEquals(2L, dashboard.get("pendentes"));
            assertEquals(1L, dashboard.get("aguardandoChefia"));
            assertEquals(10L, dashboard.get("deferidos"));
            assertEquals(20L, dashboard.get("concluidos"));
            assertEquals(1, dashboard.get("vencidos"));
            assertEquals(8L, dashboard.get("abertosNoMes"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // GERAÇÃO DE PROTOCOLO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Geração de protocolo")
    class GeracaoProtocolo {

        @Test
        @DisplayName("Deve gerar protocolo sequencial")
        void deveGerarProtocoloSequencial() {
            when(processoRepository.findMaxSequencialByAno(any())).thenReturn(5);

            String protocolo = service.gerarProtocolo();

            assertNotNull(protocolo);
            assertTrue(protocolo.contains("PROC-"));
            assertTrue(protocolo.endsWith("000006"));
        }

        @Test
        @DisplayName("Deve gerar primeiro protocolo do ano")
        void deveGerarPrimeiroProtocolo() {
            when(processoRepository.findMaxSequencialByAno(any())).thenReturn(0);

            String protocolo = service.gerarProtocolo();

            assertTrue(protocolo.endsWith("000001"));
        }
    }
}
