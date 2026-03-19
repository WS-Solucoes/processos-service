package ws.erh.cadastro.processo.service;

import org.springframework.stereotype.Component;
import ws.erh.model.cadastro.processo.Processo;
import ws.erh.model.cadastro.processo.ProcessoDocumentoModelo;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Component
public class ProcessoStoragePathResolver {

    public String resolve(Processo processo, ProcessoDocumentoModelo documentoModelo, String originalFilename) {
        String municipioSegment = resolveMunicipioSegment(processo);
        String unidadeGestoraSegment = resolveUnidadeGestoraSegment(processo);
        String servidorSegment = resolveServidorSegment(processo);
        String documentoSegment = resolveDocumentoSegment(documentoModelo);
        String protocolo = processo.getProtocolo() != null ? processo.getProtocolo() : "sem-protocolo";
        String safeFilename = uniqueFilename(originalFilename);

        return String.join("/",
                "ws-service",
                municipioSegment,
                unidadeGestoraSegment,
                servidorSegment,
                documentoSegment,
                protocolo,
                "uploads",
                safeFilename);
    }

    String resolveMunicipioSegment(Processo processo) {
        if (processo.getMunicipioNome() != null && !processo.getMunicipioNome().isBlank()) {
            return buildSegment(processo.getMunicipioNome(), processo.getUnidadeGestoraId(), "sem-municipio");
        }
        return "sem-municipio__0";
    }

    private String resolveUnidadeGestoraSegment(Processo processo) {
        if (processo.getUnidadeGestoraNome() == null || processo.getUnidadeGestoraNome().isBlank()) {
            return "sem-unidade-gestora__0";
        }
        return buildSegment(processo.getUnidadeGestoraNome(), processo.getUnidadeGestoraId(), "sem-unidade-gestora");
    }

    private String resolveServidorSegment(Processo processo) {
        if (processo.getServidorId() == null) {
            return "servidor__0";
        }
        return buildSegment(processo.getServidorNome(), processo.getServidorId(), "servidor");
    }

    private String resolveDocumentoSegment(ProcessoDocumentoModelo documentoModelo) {
        if (documentoModelo == null) {
            return "anexo-geral__0";
        }
        return buildSegment(documentoModelo.getNome(), documentoModelo.getId(), "anexo-geral");
    }

    private String buildSegment(String source, Long id, String fallback) {
        String slug = slugify(source);
        if (slug.isBlank()) {
            slug = fallback;
        }
        long safeId = id != null ? id : 0L;
        return slug + "__" + safeId;
    }

    private String uniqueFilename(String originalFilename) {
        String sanitized = sanitizeFilename(originalFilename);
        return UUID.randomUUID() + "__" + sanitized;
    }

    String sanitizeFilename(String originalFilename) {
        String filename = originalFilename;
        if (filename == null || filename.isBlank()) {
            filename = "arquivo";
        }

        filename = filename.replace("\\", "/");
        int slashIndex = filename.lastIndexOf('/');
        if (slashIndex >= 0) {
            filename = filename.substring(slashIndex + 1);
        }

        int dotIndex = filename.lastIndexOf('.');
        String base = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        String extension = dotIndex > 0 ? filename.substring(dotIndex).toLowerCase(Locale.ROOT) : "";

        String safeBase = slugify(base);
        if (safeBase.isBlank()) {
            safeBase = "arquivo";
        }

        extension = extension.replaceAll("[^a-z0-9.]", "");
        return safeBase + extension;
    }

    String slugify(String input) {
        if (input == null) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "")
                .replaceAll("-{2,}", "-");

        return normalized == null ? "" : normalized;
    }
}
