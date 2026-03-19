package ws.erh.cadastro.processo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import ws.common.config.SpringApplicationContext;
import ws.erh.model.cadastro.processo.ProcessoDocumento;

@Getter
@Setter
@NoArgsConstructor
public class ProcessoDocumentoResponse {

    private Long id;
    private Long processoId;
    private Long documentoModeloId;
    private String documentoModeloNome;
    private String nomeArquivo;
    private String tipoArquivo;
    private Long tamanhoBytes;
    private String dataEnvio;
    private String enviadoPor;
    private String situacao;
    private String situacaoDescricao;
    private String motivoRecusa;
    private String avaliadoPor;
    private String dataAvaliacao;

    public ProcessoDocumentoResponse(ProcessoDocumento entity) {
        ModelMapper modelMapper = (ModelMapper) SpringApplicationContext.getBean("modelMapper");
        modelMapper.map(entity, this);

        if (entity.getSituacao() != null) {
            this.situacao = entity.getSituacao().name();
            this.situacaoDescricao = entity.getSituacao().getDescricao();
        }

        if (entity.getDataEnvio() != null) {
            this.dataEnvio = entity.getDataEnvio().toString();
        }
        if (entity.getDataAvaliacao() != null) {
            this.dataAvaliacao = entity.getDataAvaliacao().toString();
        }

        try {
            if (entity.getProcesso() != null) {
                this.processoId = entity.getProcesso().getId();
            }
        } catch (Exception e) { }

        try {
            if (entity.getDocumentoModelo() != null) {
                this.documentoModeloId = entity.getDocumentoModelo().getId();
                this.documentoModeloNome = entity.getDocumentoModelo().getNome();
            }
        } catch (Exception e) { }
    }
}
