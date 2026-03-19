package ws.erh.cadastro.processo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProcessoComplementacaoRespostaRequest {

    private String dadosFormulario;
    private String observacaoServidor;
    private String prioridade;
    private List<AnexoRespostaRequest> anexos = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AnexoRespostaRequest {
        private Long itemId;
        private Long documentoModeloId;
        private String label;
    }
}
