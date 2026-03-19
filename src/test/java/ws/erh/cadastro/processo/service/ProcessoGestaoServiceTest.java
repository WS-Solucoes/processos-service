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
import ws.erh.cadastro.portal.service.PortalNotificacaoServiceInterface;
import ws.erh.cadastro.processo.repository.ProcessoHistoricoRepository;
import ws.erh.cadastro.processo.repository.ProcessoRepository;
import ws.erh.core.enums.processo.ResultadoProcesso;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.core.exception.EntityNotFoundException;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoModelo;
import ws.erh.model.cadastro.servidor.Servidor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessoGestaoService - Testes unitários")
class ProcessoGestaoServiceTest {

    @InjectMocks
    private ProcessoGestaoService service;

    @Mock
    private ProcessoRepository processoRepository;

    @Mock
    private ProcessoHistoricoRepository historicoRepository;

    @Mock
    private PortalNotificacaoServiceInterface notificacaoService;

    @Mock
    private ProcessoWorkflowService workflowService;

    @Mock
    private ProcessoValidacaoService validacaoService;

    @Mock
    private ProcessoComplementacaoService complementacaoService;

    @Mock
    private UnidadeGestoraRepository unidadeGestoraRepository;

    @Mock
    private UsuarioDetailsService usuarioDetailsService;

    private Processo processo;

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

        Servidor servidor = new Servidor();
        servidor.setId(1L);

        ProcessoModelo modelo = new ProcessoModelo();
        modelo.setId(1L);

        processo = new Processo();
        processo.setId(1L);
        processo.setProtocolo("PROC-2025-000001");
        processo.setServidor(servidor);
        processo.setProcessoModelo(modelo);
        processo.setSituacao(SituacaoProcesso.ABERTO);
        processo.setEtapaAtual(1);
        processo.setDataAbertura(LocalDateTime.now().minusDays(5));
        processo.setDataUltimaAtualizacao(LocalDateTime.now().minusDays(1));

        lenient().doAnswer(invocation -> {
            Processo entity = invocation.getArgument(0);
            entity.setSituacao(SituacaoProcesso.EM_ANALISE);
            return null;
        }).when(workflowService).iniciarAnalise(any(Processo.class));

        lenient().doAnswer(invocation -> {
            Processo entity = invocation.getArgument(0);
            entity.setSituacao(SituacaoProcesso.PENDENTE_DOCUMENTACAO);
            return null;
        }).when(workflowService).solicitarComplementacao(any(Processo.class));

        lenient().doAnswer(invocation -> {
            Processo entity = invocation.getArgument(0);
            entity.setSituacao(SituacaoProcesso.AGUARDANDO_CHEFIA);
            entity.setEtapaAtual(2);
            return null;
        }).when(workflowService).encaminharChefia(any(Processo.class));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════════
    // ATRIBUIÇÃO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Atribuição de processo")
    class AtribuicaoProcesso {

        @Test
        @DisplayName("Deve atribuir processo a um analista")
        void deveAtribuirProcesso() {
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
            when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Processo result = service.atribuir(1L, "analista.rh", "Departamento RH", "admin");

            assertEquals("analista.rh", result.getAtribuidoPara());
            assertEquals("Departamento RH", result.getDepartamentoAtribuido());
            assertNotNull(result.getDataUltimaAtualizacao());
            verify(historicoRepository).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // INÍCIO DE ANÁLISE
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Início de análise")
    class InicioAnalise {

        @Test
        @DisplayName("Deve iniciar análise de processo aberto")
        void deveIniciarAnalise() {
            processo.setSituacao(SituacaoProcesso.ABERTO);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
            when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Processo result = service.iniciarAnalise(1L, "analista.rh");

            assertEquals(SituacaoProcesso.EM_ANALISE, result.getSituacao());
            verify(historicoRepository).save(any());
        }

        @Test
        @DisplayName("Deve rejeitar análise de processo concluído")
        void deveRejeitarAnaliseConcluido() {
            processo.setSituacao(SituacaoProcesso.CONCLUIDO);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

            assertThrows(IllegalStateException.class, () -> service.iniciarAnalise(1L, "analista"));
        }

        @Test
        @DisplayName("Deve rejeitar análise de processo cancelado")
        void deveRejeitarAnaliseCancelado() {
            processo.setSituacao(SituacaoProcesso.CANCELADO);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

            assertThrows(IllegalStateException.class, () -> service.iniciarAnalise(1L, "analista"));
        }

        @Test
        @DisplayName("Deve rejeitar análise de processo arquivado")
        void deveRejeitarAnaliseArquivado() {
            processo.setSituacao(SituacaoProcesso.ARQUIVADO);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

            assertThrows(IllegalStateException.class, () -> service.iniciarAnalise(1L, "analista"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SOLICITAÇÃO DE DOCUMENTAÇÃO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Solicitação de documentação")
    class SolicitacaoDocumentacao {

        @Test
        @DisplayName("Deve solicitar documentação no processo")
        void deveSolicitarDocumentacao() {
            processo.setSituacao(SituacaoProcesso.EM_ANALISE);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
            when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Processo result = service.solicitarDocumentacao(1L, "Enviar RG e CPF", "analista");

            assertEquals(SituacaoProcesso.PENDENTE_DOCUMENTACAO, result.getSituacao());
            verify(historicoRepository).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ENCAMINHAMENTO PARA CHEFIA
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Encaminhamento para chefia")
    class EncaminhamentoChefia {

        @Test
        @DisplayName("Deve encaminhar processo para chefia")
        void deveEncaminharParaChefia() {
            processo.setSituacao(SituacaoProcesso.EM_ANALISE);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
            when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Processo result = service.encaminharChefia(1L, "analista");

            assertEquals(SituacaoProcesso.AGUARDANDO_CHEFIA, result.getSituacao());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DEFERIMENTO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Deferimento")
    class Deferimento {

        @Test
        @DisplayName("Deve deferir processo em análise")
        void deveDeferirProcesso() {
            processo.setSituacao(SituacaoProcesso.EM_ANALISE);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
            when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Processo result = service.deferir(1L, "Processo aprovado conforme análise", "analista");

            assertEquals(SituacaoProcesso.DEFERIDO, result.getSituacao());
            assertEquals(ResultadoProcesso.DEFERIDO, result.getResultado());
            assertEquals("Processo aprovado conforme análise", result.getJustificativaResultado());
        }

        @Test
        @DisplayName("Deve rejeitar deferimento de processo concluído")
        void deveRejeitarDeferimentoConcluido() {
            processo.setSituacao(SituacaoProcesso.CONCLUIDO);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

            assertThrows(IllegalStateException.class, () -> service.deferir(1L, "Motivo", "analista"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // INDEFERIMENTO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Indeferimento")
    class Indeferimento {

        @Test
        @DisplayName("Deve indeferir processo com justificativa")
        void deveIndeferirComJustificativa() {
            processo.setSituacao(SituacaoProcesso.EM_ANALISE);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
            when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Processo result = service.indeferir(1L, "Documentação insuficiente", "analista");

            assertEquals(SituacaoProcesso.INDEFERIDO, result.getSituacao());
            assertEquals(ResultadoProcesso.INDEFERIDO, result.getResultado());
            assertEquals("Documentação insuficiente", result.getJustificativaResultado());
            assertNotNull(result.getDataConclusao());
        }

        @Test
        @DisplayName("Deve rejeitar indeferimento sem justificativa")
        void deveRejeitarSemJustificativa() {
            processo.setSituacao(SituacaoProcesso.EM_ANALISE);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

            assertThrows(IllegalStateException.class, () -> service.indeferir(1L, null, "analista"));
        }

        @Test
        @DisplayName("Deve rejeitar indeferimento com justificativa em branco")
        void deveRejeitarJustificativaEmBranco() {
            processo.setSituacao(SituacaoProcesso.EM_ANALISE);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

            assertThrows(IllegalStateException.class, () -> service.indeferir(1L, "   ", "analista"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DEVOLUÇÃO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Devolução ao servidor")
    class Devolucao {

        @Test
        @DisplayName("Deve devolver processo ao servidor")
        void deveDevolverProcesso() {
            processo.setSituacao(SituacaoProcesso.EM_ANALISE);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
            when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Processo result = service.devolver(1L, "Ajustar dados do formulário", "analista");

            assertEquals(SituacaoProcesso.PENDENTE_DOCUMENTACAO, result.getSituacao());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // EXECUÇÃO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Execução do processo")
    class Execucao {

        @Test
        @DisplayName("Deve marcar processo como em execução")
        void deveMarcarEmExecucao() {
            processo.setSituacao(SituacaoProcesso.DEFERIDO);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
            when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Processo result = service.executar(1L, "sistema");

            assertEquals(SituacaoProcesso.EM_EXECUCAO, result.getSituacao());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // CONCLUSÃO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Conclusão do processo")
    class Conclusao {

        @Test
        @DisplayName("Deve concluir processo em execução")
        void deveConcluirProcesso() {
            processo.setSituacao(SituacaoProcesso.EM_EXECUCAO);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
            when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Processo result = service.concluir(1L, "sistema");

            assertEquals(SituacaoProcesso.CONCLUIDO, result.getSituacao());
            assertNotNull(result.getDataConclusao());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ARQUIVAMENTO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Arquivamento")
    class Arquivamento {

        @Test
        @DisplayName("Deve arquivar processo aberto")
        void deveArquivarProcesso() {
            processo.setSituacao(SituacaoProcesso.ABERTO);
            when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
            when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(historicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Processo result = service.arquivar(1L, "admin");

            assertEquals(SituacaoProcesso.ARQUIVADO, result.getSituacao());
            assertEquals(ResultadoProcesso.ARQUIVADO, result.getResultado());
            assertNotNull(result.getDataConclusao());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PROCESSO NÃO ENCONTRADO
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Processo não encontrado")
    class ProcessoNaoEncontrado {

        @Test
        @DisplayName("Deve lançar exceção ao atribuir processo inexistente")
        void deveLancarExcecaoAtribuirInexistente() {
            when(processoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.atribuir(99L, "analista", "RH", "admin"));
        }

        @Test
        @DisplayName("Deve lançar exceção ao iniciar análise de processo inexistente")
        void deveLancarExcecaoAnalisarInexistente() {
            when(processoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.iniciarAnalise(99L, "analista"));
        }
    }
}
