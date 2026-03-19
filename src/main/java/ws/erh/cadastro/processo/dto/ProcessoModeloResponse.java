package ws.erh.cadastro.processo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import ws.common.config.SpringApplicationContext;
import ws.erh.model.cadastro.processo.ProcessoCampoModelo;
import ws.erh.model.cadastro.processo.ProcessoDocumentoModelo;
import ws.erh.model.cadastro.processo.ProcessoEtapaModelo;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class ProcessoModeloResponse {

    private Long id;
    private String codigo;
    private String nome;
    private String descricao;
    private String instrucoes;
    private String categoria;
    private String categoriaDescricao;
    private String icone;
    private String cor;
    private Integer prazoAtendimentoDias;
    private Boolean requerAprovacaoChefia;
    private Boolean geraAcaoAutomatica;
    private Boolean ativo;
    private Boolean visivelPortal;
    private Integer ordemExibicao;

    private List<DocumentoModeloResponse> documentosExigidos = new ArrayList<>();
    private List<EtapaModeloResponse> etapas = new ArrayList<>();
    private List<CampoModeloResponse> camposAdicionais = new ArrayList<>();

    public ProcessoModeloResponse(ProcessoModelo entity) {
        ModelMapper modelMapper = (ModelMapper) SpringApplicationContext.getBean("modelMapper");
        modelMapper.map(entity, this);

        if (entity.getCategoria() != null) {
            this.categoria = entity.getCategoria().name();
            this.categoriaDescricao = entity.getCategoria().getDescricao();
        }

        try {
            if (entity.getDocumentosExigidos() != null) {
                this.documentosExigidos = entity.getDocumentosExigidos().stream()
                        .map(DocumentoModeloResponse::new)
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {
        }

        try {
            if (entity.getEtapas() != null) {
                this.etapas = entity.getEtapas().stream()
                        .map(EtapaModeloResponse::new)
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {
        }

        try {
            if (entity.getCamposAdicionais() != null) {
                this.camposAdicionais = entity.getCamposAdicionais().stream()
                        .map(CampoModeloResponse::new)
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DocumentoModeloResponse {
        private Long id;
        private String nome;
        private String descricao;
        private Boolean obrigatorio;
        private String tiposPermitidos;
        private Integer tamanhoMaximoMb;
        private String modeloUrl;
        private Integer ordem;
        private Long etapaModeloId;
        private Integer etapaOrdem;
        private String etapaNome;

        public DocumentoModeloResponse(ProcessoDocumentoModelo entity) {
            ModelMapper modelMapper = (ModelMapper) SpringApplicationContext.getBean("modelMapper");
            modelMapper.map(entity, this);
            try {
                if (entity.getEtapaModelo() != null) {
                    this.etapaModeloId = entity.getEtapaModelo().getId();
                    this.etapaOrdem = entity.getEtapaModelo().getOrdem();
                    this.etapaNome = entity.getEtapaModelo().getNome();
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EtapaModeloResponse {
        private Long id;
        private String nome;
        private String descricao;
        private Integer ordem;
        private String tipoResponsavel;
        private String acaoAutomatica;
        private Integer prazoDias;

        public EtapaModeloResponse(ProcessoEtapaModelo entity) {
            ModelMapper modelMapper = (ModelMapper) SpringApplicationContext.getBean("modelMapper");
            modelMapper.map(entity, this);
            if (entity.getTipoResponsavel() != null) {
                this.tipoResponsavel = entity.getTipoResponsavel().name();
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CampoModeloResponse {
        private Long id;
        private String nomeCampo;
        private String label;
        private String tipoCampo;
        private Boolean obrigatorio;
        private String opcoesSelect;
        private String placeholder;
        private String ajuda;
        private Integer ordem;
        private Long etapaModeloId;
        private Integer etapaOrdem;
        private String etapaNome;

        public CampoModeloResponse(ProcessoCampoModelo entity) {
            ModelMapper modelMapper = (ModelMapper) SpringApplicationContext.getBean("modelMapper");
            modelMapper.map(entity, this);
            if (entity.getTipoCampo() != null) {
                this.tipoCampo = entity.getTipoCampo().name();
            }
            try {
                if (entity.getEtapaModelo() != null) {
                    this.etapaModeloId = entity.getEtapaModelo().getId();
                    this.etapaOrdem = entity.getEtapaModelo().getOrdem();
                    this.etapaNome = entity.getEtapaModelo().getNome();
                }
            } catch (Exception ignored) {
            }
        }
    }
}
