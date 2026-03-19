package ws.erh.cadastro.processo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import ws.common.config.SpringApplicationContext;
import ws.erh.model.cadastro.processo.ProcessoHistorico;

@Getter
@Setter
@NoArgsConstructor
public class ProcessoHistoricoResponse {

    private Long id;
    private Long processoId;
    private String dataHora;
    private String acao;
    private String acaoDescricao;
    private String situacaoAnterior;
    private String situacaoNova;
    private Integer etapaAnterior;
    private Integer etapaNova;
    private String usuario;
    private String tipoUsuario;
    private String descricao;
    private String dadosExtras;

    public ProcessoHistoricoResponse(ProcessoHistorico entity) {
        ModelMapper modelMapper = (ModelMapper) SpringApplicationContext.getBean("modelMapper");
        modelMapper.map(entity, this);

        if (entity.getAcao() != null) {
            this.acao = entity.getAcao().name();
            this.acaoDescricao = entity.getAcao().getDescricao();
        }
        if (entity.getTipoUsuario() != null) {
            this.tipoUsuario = entity.getTipoUsuario().name();
        }
        if (entity.getDataHora() != null) {
            this.dataHora = entity.getDataHora().toString();
        }

        try {
            if (entity.getProcesso() != null) {
                this.processoId = entity.getProcesso().getId();
            }
        } catch (Exception e) { }
    }
}
