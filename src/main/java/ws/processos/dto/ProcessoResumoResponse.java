package ws.processos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProcessoResumoResponse {
    private Long servidorId;
    private Long unidadeGestoraId;
    private long processosAbertos;
}
