package ws.erh.cadastro.processo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import ws.erh.cadastro.processo.dto.ProcessoPendenciaDocumentalResponse;
import ws.erh.core.enums.processo.SituacaoDocumento;
import ws.erh.core.enums.processo.TipoCampo;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoCampoModelo;
import ws.erh.model.cadastro.processo.ProcessoDocumento;
import ws.erh.model.cadastro.processo.ProcessoDocumentoModelo;
import ws.erh.model.cadastro.processo.ProcessoModelo;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProcessoValidacaoService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void validarSubmissao(ProcessoModelo modelo, String dadosFormulario, List<Long> documentoModeloIds) {
        validarCamposObrigatorios(modelo, dadosFormulario);
        validarSchemaFormulario(modelo, dadosFormulario);
        validarDocumentosObrigatorios(modelo, documentoModeloIds);
    }

    public void validarProcessoParaDeferimento(Processo processo) {
        validarCamposObrigatorios(processo.getProcessoModelo(), processo.getDadosFormulario());
        validarSchemaFormulario(processo.getProcessoModelo(), processo.getDadosFormulario());

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

    /**
     * Valida o conteúdo de {@code dadosFormulario} contra o schema declarado em
     * {@link ProcessoModelo#getCamposAdicionais()} (S6.9).
     *
     * <p>Para cada campo presente nos dados (campos ausentes / vazios não-obrigatórios
     * são ignorados aqui — a obrigatoriedade é tratada por
     * {@link #validarCamposObrigatorios}), checa:</p>
     * <ul>
     *   <li>{@link TipoCampo#NUMBER}: valor parseável como número.</li>
     *   <li>{@link TipoCampo#DATE}: valor ISO {@code yyyy-MM-dd}.</li>
     *   <li>{@link TipoCampo#BOOLEAN}: valor {@code true}/{@code false} (Boolean ou string).</li>
     *   <li>{@link TipoCampo#SELECT}: valor presente em {@code opcoesSelect} (separador {@code |}).</li>
     *   <li>{@link TipoCampo#TEXT}/{@link TipoCampo#TEXTAREA}: qualquer string aceita.</li>
     * </ul>
     *
     * <p>Aglutina todos os erros e lança {@link IllegalStateException} única
     * com a lista para que o frontend mostre tudo de uma vez.</p>
     */
    public void validarSchemaFormulario(ProcessoModelo modelo, String dadosFormulario) {
        if (modelo == null || modelo.getCamposAdicionais() == null || modelo.getCamposAdicionais().isEmpty()) {
            return;
        }
        Map<String, Object> dados = parseFormulario(dadosFormulario);
        if (dados.isEmpty()) {
            return;
        }

        List<String> erros = new ArrayList<>();
        for (ProcessoCampoModelo campo : modelo.getCamposAdicionais()) {
            if (campo.getNomeCampo() == null || campo.getTipoCampo() == null) {
                continue;
            }
            Object valor = dados.get(campo.getNomeCampo());
            if (isBlankValue(valor)) {
                continue; // ausente/vazio: presença é verificada à parte
            }
            String erro = validarValorCampo(campo, valor);
            if (erro != null) {
                erros.add(erro);
            }
        }
        if (!erros.isEmpty()) {
            throw new IllegalStateException("Campos do formulário inválidos: " + String.join("; ", erros));
        }
    }

    private String validarValorCampo(ProcessoCampoModelo campo, Object valor) {
        String label = campo.getLabel() != null ? campo.getLabel() : campo.getNomeCampo();
        switch (campo.getTipoCampo()) {
            case NUMBER:
                if (valor instanceof Number) {
                    return null;
                }
                try {
                    Double.parseDouble(String.valueOf(valor).trim());
                    return null;
                } catch (NumberFormatException ex) {
                    return label + " deve ser numérico";
                }
            case DATE:
                try {
                    LocalDate.parse(String.valueOf(valor).trim());
                    return null;
                } catch (DateTimeParseException ex) {
                    return label + " deve estar no formato yyyy-MM-dd";
                }
            case BOOLEAN:
                if (valor instanceof Boolean) {
                    return null;
                }
                String s = String.valueOf(valor).trim().toLowerCase();
                if ("true".equals(s) || "false".equals(s)) {
                    return null;
                }
                return label + " deve ser true ou false";
            case SELECT:
                Set<String> opcoes = parseOpcoesSelect(campo.getOpcoesSelect());
                if (opcoes.isEmpty()) {
                    return null; // sem opcões declaradas: aceita qualquer string
                }
                if (!opcoes.contains(String.valueOf(valor).trim())) {
                    return label + " deve ser uma das opções: " + String.join(" | ", opcoes);
                }
                return null;
            case TEXT:
            case TEXTAREA:
            default:
                return null;
        }
    }

    private static Set<String> parseOpcoesSelect(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
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
