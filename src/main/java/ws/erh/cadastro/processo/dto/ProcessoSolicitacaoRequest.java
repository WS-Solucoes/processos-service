package ws.erh.cadastro.processo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProcessoSolicitacaoRequest {

    @NotNull(message = "Modelo de processo é obrigatório")
    private Long processoModeloId;

    @NotNull(message = "Servidor é obrigatório")
    private Long servidorId;

    private Long vinculoFuncionalId;
    private String dadosFormulario;
    private String mensagemInicial;
    private String prioridade;
    private String prazoLimite;
}
