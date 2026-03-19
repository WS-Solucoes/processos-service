package ws.erh.cadastro.processo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProcessoComplementacaoRequest {

    private String prazoLimite;
    private String motivoConsolidado;
    private List<ItemRequest> itens = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ItemRequest {
        private String tipoItem;
        private Long documentoModeloId;
        private Long campoModeloId;
        private String label;
        private Boolean obrigatorio;
        private String motivo;
        private Integer ordem;
    }
}
