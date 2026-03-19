package ws.erh.cadastro.processo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import ws.common.config.SpringApplicationContext;
import ws.erh.model.cadastro.processo.ProcessoMensagem;

@Getter
@Setter
@NoArgsConstructor
public class ProcessoMensagemResponse {

    private Long id;
    private Long processoId;
    private String autor;
    private String tipoAutor;
    private String mensagem;
    private String dataHora;
    private Boolean lida;
    private String dataLeitura;
    private String anexoNome;
    private String anexoTipo;
    private Boolean temAnexo;

    public ProcessoMensagemResponse(ProcessoMensagem entity) {
        ModelMapper modelMapper = (ModelMapper) SpringApplicationContext.getBean("modelMapper");
        modelMapper.map(entity, this);

        if (entity.getTipoAutor() != null) {
            this.tipoAutor = entity.getTipoAutor().name();
        }
        if (entity.getDataHora() != null) {
            this.dataHora = entity.getDataHora().toString();
        }
        if (entity.getDataLeitura() != null) {
            this.dataLeitura = entity.getDataLeitura().toString();
        }

        try {
            if (entity.getProcesso() != null) {
                this.processoId = entity.getProcesso().getId();
            }
        } catch (Exception e) { }

        this.temAnexo = entity.getAnexoNome() != null && !entity.getAnexoNome().isEmpty();
    }
}
