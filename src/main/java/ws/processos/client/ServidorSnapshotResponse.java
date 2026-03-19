package ws.processos.client;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServidorSnapshotResponse {
    private Long id;
    private String nome;
    private String cpf;
    private Long unidadeGestoraId;
    private String unidadeGestoraNome;
    private String municipioNome;
}
