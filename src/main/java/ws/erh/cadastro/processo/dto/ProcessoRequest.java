package ws.erh.cadastro.processo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProcessoRequest {

    @NotNull(message = "Modelo de processo é obrigatório")
    private Long processoModeloId;

    @NotNull(message = "Servidor é obrigatório")
    private Long servidorId;

    private Long vinculoFuncionalId;

    private String dadosFormulario;

    private String observacaoServidor;

    private String prioridade;
}
