package ws.erh.cadastro.processo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ws.erh.cadastro.processo.repository.ProcessoRepository;
import ws.erh.core.enums.processo.SituacaoProcesso;
import ws.erh.model.cadastro.processo.Processo;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessoAutoArquivamentoScheduler - Testes unitários")
class ProcessoAutoArquivamentoSchedulerTest {

    @InjectMocks
    private ProcessoAutoArquivamentoScheduler scheduler;

    @Mock
    private ProcessoRepository processoRepository;

    @Mock
    private ProcessoGestaoServiceInterface processoGestaoService;

    private Processo processoConcluido;
    private Processo processoPendente;
    private Processo processoRecente;

    @BeforeEach
    void setUp() {
        processoConcluido = new Processo();
        processoConcluido.setId(1L);
        processoConcluido.setProtocolo("PROC-2025-000001");
        processoConcluido.setSituacao(SituacaoProcesso.CONCLUIDO);
        processoConcluido.setDataAbertura(LocalDateTime.now().minusDays(120));
        processoConcluido.setDataUltimaAtualizacao(LocalDateTime.now().minusDays(100));

        processoPendente = new Processo();
        processoPendente.setId(2L);
        processoPendente.setProtocolo("PROC-2025-000002");
        processoPendente.setSituacao(SituacaoProcesso.PENDENTE_DOCUMENTACAO);
        processoPendente.setDataAbertura(LocalDateTime.now().minusDays(60));
        processoPendente.setDataUltimaAtualizacao(LocalDateTime.now().minusDays(45));

        processoRecente = new Processo();
        processoRecente.setId(3L);
        processoRecente.setProtocolo("PROC-2025-000003");
        processoRecente.setSituacao(SituacaoProcesso.CONCLUIDO);
        processoRecente.setDataAbertura(LocalDateTime.now().minusDays(10));
        processoRecente.setDataUltimaAtualizacao(LocalDateTime.now().minusDays(5));
    }

    @Nested
    @DisplayName("Arquivamento por situação")
    class ArquivamentoPorSituacao {

        @Test
        @DisplayName("Deve arquivar processo concluído há mais de 90 dias")
        void deveArquivarConcluidoAntigo() {
            when(processoRepository.findBySituacao(SituacaoProcesso.CONCLUIDO))
                    .thenReturn(List.of(processoConcluido));
            when(processoGestaoService.arquivar(1L, "SISTEMA")).thenReturn(processoConcluido);

            int count = scheduler.arquivarPorSituacao(SituacaoProcesso.CONCLUIDO, 90, "Auto-arquivado");

            assertEquals(1, count);
            verify(processoGestaoService).arquivar(1L, "SISTEMA");
        }

        @Test
        @DisplayName("Não deve arquivar processo concluído recentemente")
        void naoDeveArquivarConcluidoRecente() {
            when(processoRepository.findBySituacao(SituacaoProcesso.CONCLUIDO))
                    .thenReturn(List.of(processoRecente));

            int count = scheduler.arquivarPorSituacao(SituacaoProcesso.CONCLUIDO, 90, "Auto-arquivado");

            assertEquals(0, count);
            verify(processoGestaoService, never()).arquivar(any(), any());
        }

        @Test
        @DisplayName("Deve retornar 0 quando não há processos")
        void deveRetornarZeroSemProcessos() {
            when(processoRepository.findBySituacao(SituacaoProcesso.CONCLUIDO))
                    .thenReturn(Collections.emptyList());

            int count = scheduler.arquivarPorSituacao(SituacaoProcesso.CONCLUIDO, 90, "Auto-arquivado");

            assertEquals(0, count);
        }

        @Test
        @DisplayName("Deve continuar mesmo em caso de erro em um processo")
        void deveContinuarEmCasoDeErro() {
            Processo outroProcesso = new Processo();
            outroProcesso.setId(4L);
            outroProcesso.setProtocolo("PROC-2025-000004");
            outroProcesso.setDataUltimaAtualizacao(LocalDateTime.now().minusDays(100));

            when(processoRepository.findBySituacao(SituacaoProcesso.CONCLUIDO))
                    .thenReturn(List.of(processoConcluido, outroProcesso));
            when(processoGestaoService.arquivar(1L, "SISTEMA")).thenThrow(new RuntimeException("Erro"));
            when(processoGestaoService.arquivar(4L, "SISTEMA")).thenReturn(outroProcesso);

            int count = scheduler.arquivarPorSituacao(SituacaoProcesso.CONCLUIDO, 90, "Auto-arquivado");

            assertEquals(1, count);
        }
    }

    @Nested
    @DisplayName("Arquivamento de pendentes inativos")
    class ArquivamentoPendentesInativos {

        @Test
        @DisplayName("Deve arquivar processo pendente de documentação há mais de 30 dias")
        void deveArquivarPendenteInativo() {
            when(processoRepository.findBySituacao(SituacaoProcesso.PENDENTE_DOCUMENTACAO))
                    .thenReturn(List.of(processoPendente));
            when(processoGestaoService.arquivar(2L, "SISTEMA")).thenReturn(processoPendente);

            int count = scheduler.arquivarPendentesInativos(30);

            assertEquals(1, count);
            verify(processoGestaoService).arquivar(2L, "SISTEMA");
        }

        @Test
        @DisplayName("Não deve arquivar processo pendente recente")
        void naoDeveArquivarPendenteRecente() {
            Processo pendenteRecente = new Processo();
            pendenteRecente.setId(5L);
            pendenteRecente.setProtocolo("PROC-2025-000005");
            pendenteRecente.setDataUltimaAtualizacao(LocalDateTime.now().minusDays(10));

            when(processoRepository.findBySituacao(SituacaoProcesso.PENDENTE_DOCUMENTACAO))
                    .thenReturn(List.of(pendenteRecente));

            int count = scheduler.arquivarPendentesInativos(30);

            assertEquals(0, count);
            verify(processoGestaoService, never()).arquivar(any(), any());
        }
    }

    @Nested
    @DisplayName("Execução completa do agendamento")
    class ExecucaoCompleta {

        @Test
        @DisplayName("Deve executar auto-arquivamento completo sem erros")
        void deveExecutarAutoArquivamentoCompleto() {
            when(processoRepository.findBySituacao(SituacaoProcesso.CONCLUIDO))
                    .thenReturn(List.of(processoConcluido));
            when(processoRepository.findBySituacao(SituacaoProcesso.CANCELADO))
                    .thenReturn(Collections.emptyList());
            when(processoRepository.findBySituacao(SituacaoProcesso.PENDENTE_DOCUMENTACAO))
                    .thenReturn(List.of(processoPendente));
            when(processoGestaoService.arquivar(any(), eq("SISTEMA"))).thenAnswer(inv -> {
                Processo p = new Processo();
                p.setId(inv.getArgument(0));
                return p;
            });

            assertDoesNotThrow(() -> scheduler.autoArquivarProcessos());

            verify(processoGestaoService, atLeast(1)).arquivar(any(), eq("SISTEMA"));
        }
    }
}
