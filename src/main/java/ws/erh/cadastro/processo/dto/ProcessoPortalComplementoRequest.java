package ws.erh.cadastro.processo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProcessoPortalComplementoRequest {

    private String dadosFormulario;
    private String observacaoServidor;
    private String prioridade;
}
