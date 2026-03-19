package ws.erh.cadastro.processo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProcessoMensagemRequest {

    @NotNull(message = "Processo é obrigatório")
    private Long processoId;

    @NotBlank(message = "Autor é obrigatório")
    private String autor;

    @NotNull(message = "Tipo de autor é obrigatório")
    private String tipoAutor;

    @NotBlank(message = "Mensagem é obrigatória")
    private String mensagem;
}
