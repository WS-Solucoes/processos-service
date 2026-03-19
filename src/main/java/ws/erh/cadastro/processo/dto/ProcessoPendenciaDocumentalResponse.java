package ws.erh.cadastro.processo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProcessoPendenciaDocumentalResponse {

    private Long documentoModeloId;
    private String nomeDocumento;
    private Boolean obrigatorio;
    private String situacao;
    private String situacaoDescricao;
    private Boolean possuiArquivo;
    private Long ultimoDocumentoId;
    private String motivoRecusa;
}
