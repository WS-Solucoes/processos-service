package ws.erh.cadastro.processo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import ws.erh.cadastro.processo.dto.ProcessoPendenciaDocumentalResponse;
import ws.erh.core.enums.processo.SituacaoDocumento;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoCampoModelo;
import ws.erh.model.cadastro.processo.ProcessoDocumento;
import ws.erh.model.cadastro.processo.ProcessoDocumentoModelo;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProcessoValidacaoService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void validarSubmissao(ProcessoModelo modelo, String dadosFormulario, List<Long> documentoModeloIds) {
        validarCamposObrigatorios(modelo, dadosFormulario);
        validarDocumentosObrigatorios(modelo, documentoModeloIds);
    }

    public void validarProcessoParaDeferimento(Processo processo) {
        validarCamposObrigatorios(processo.getProcessoModelo(), processo.getDadosFormulario());

        List<ProcessoPendenciaDocumentalResponse> pendencias = calcularPendencias(processo);
        List<String> obrigatoriosInvalidos = pendencias.stream()
                .filter(p -> Boolean.TRUE.equals(p.getObrigatorio()))
                .filter(p -> !"ACEITO".equals(p.getSituacao()) && !"PENDENTE".equals(p.getSituacao()))
                .map(ProcessoPendenciaDocumentalResponse::getNomeDocumento)
                .toList();

        if (!obrigatoriosInvalidos.isEmpty()) {
            throw new IllegalStateException("Documentos obrigatórios pendentes ou recusados: " + String.join(", ", obrigatoriosInvalidos));
        }
    }

    public List<ProcessoPendenciaDocumentalResponse> calcularPendencias(Processo processo) {
        ProcessoModelo modelo = processo.getProcessoModelo();
        if (modelo == null || modelo.getDocumentosExigidos() == null || modelo.getDocumentosExigidos().isEmpty()) {
            return List.of();
        }

        Map<Long, ProcessoDocumento> ultimoDocumentoPorModelo = processo.getDocumentos() == null
                ? Map.of()
                : processo.getDocumentos().stream()
                .filter(doc -> doc.getDocumentoModelo() != null && doc.getDocumentoModelo().getId() != null)
                .collect(Collectors.toMap(
                        doc -> doc.getDocumentoModelo().getId(),
                        doc -> doc,
                        (left, right) -> left.getDataEnvio() != null && right.getDataEnvio() != null && left.getDataEnvio().isAfter(right.getDataEnvio()) ? left : right,
                        LinkedHashMap::new));

        List<ProcessoPendenciaDocumentalResponse> responses = new ArrayList<>();
        for (ProcessoDocumentoModelo documentoModelo : modelo.getDocumentosExigidos().stream()
                .sorted(Comparator.comparing(ProcessoDocumentoModelo::getOrdem, Comparator.nullsLast(Integer::compareTo)))
                .toList()) {
            ProcessoDocumento doc = documentoModelo.getId() != null
                    ? ultimoDocumentoPorModelo.get(documentoModelo.getId())
                    : null;

            ProcessoPendenciaDocumentalResponse response = new ProcessoPendenciaDocumentalResponse();
            response.setDocumentoModeloId(documentoModelo.getId());
            response.setNomeDocumento(documentoModelo.getNome());
            response.setObrigatorio(Boolean.TRUE.equals(documentoModelo.getObrigatorio()));
            response.setPossuiArquivo(doc != null);
            response.setUltimoDocumentoId(doc != null ? doc.getId() : null);
            response.setMotivoRecusa(doc != null ? doc.getMotivoRecusa() : null);

            if (doc == null) {
                response.setSituacao("FALTANTE");
                response.setSituacaoDescricao("Documento pendente de envio");
            } else {
                SituacaoDocumento situacao = doc.getSituacao() != null ? doc.getSituacao() : SituacaoDocumento.PENDENTE;
                response.setSituacao(situacao.name());
                response.setSituacaoDescricao(situacao.getDescricao());
            }

            responses.add(response);
        }

        return responses;
    }

    private void validarCamposObrigatorios(ProcessoModelo modelo, String dadosFormulario) {
        if (modelo == null || modelo.getCamposAdicionais() == null || modelo.getCamposAdicionais().isEmpty()) {
            return;
        }

        Map<String, Object> dados = parseFormulario(dadosFormulario);
        List<String> faltantes = modelo.getCamposAdicionais().stream()
                .filter(campo -> Boolean.TRUE.equals(campo.getObrigatorio()))
                .map(ProcessoCampoModelo::getNomeCampo)
                .filter(Objects::nonNull)
                .filter(nome -> isBlankValue(dados.get(nome)))
                .toList();

        if (!faltantes.isEmpty()) {
            throw new IllegalStateException("Campos obrigatórios não informados: " + String.join(", ", faltantes));
        }
    }

    private void validarDocumentosObrigatorios(ProcessoModelo modelo, List<Long> documentoModeloIds) {
        if (modelo == null || modelo.getDocumentosExigidos() == null || modelo.getDocumentosExigidos().isEmpty()) {
            return;
        }

        List<Long> enviados = documentoModeloIds == null ? List.of() : documentoModeloIds;
        List<String> faltantes = modelo.getDocumentosExigidos().stream()
                .filter(doc -> Boolean.TRUE.equals(doc.getObrigatorio()))
                .filter(doc -> doc.getId() != null)
                .filter(doc -> !enviados.contains(doc.getId()))
                .map(ProcessoDocumentoModelo::getNome)
                .toList();

        if (!faltantes.isEmpty()) {
            throw new IllegalStateException("Documentos obrigatórios não enviados: " + String.join(", ", faltantes));
        }
    }

    private Map<String, Object> parseFormulario(String dadosFormulario) {
        if (dadosFormulario == null || dadosFormulario.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(dadosFormulario, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Dados do formulário inválidos.");
        }
    }

    private boolean isBlankValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        return false;
    }
}
