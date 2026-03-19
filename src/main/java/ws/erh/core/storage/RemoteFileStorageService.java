package ws.erh.core.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Implementação de ArquivoStorageService que delega ao file-storage-service
 * via Feign Client (microsserviço remoto na porta 9085).
 *
 * Ativado quando ws.erh.storage.type=REMOTE (padrão no Docker).
 * Mantém total compatibilidade com a interface ArquivoStorageService usada
 * pelos controllers e services existentes.
 */
@Service
@ConditionalOnProperty(prefix = "ws.erh.storage", name = "type", havingValue = "REMOTE", matchIfMissing = true)
public class RemoteFileStorageService implements ArquivoStorageService {

    private static final Logger log = LoggerFactory.getLogger(RemoteFileStorageService.class);

    private final FileStorageClient fileStorageClient;

    public RemoteFileStorageService(FileStorageClient fileStorageClient) {
        this.fileStorageClient = fileStorageClient;
        log.info("RemoteFileStorageService inicializado - delegando ao file-storage-service");
    }

    @Override
    public OutputStream getOutputStream(String relativePath) {
        // Retorna um ByteArrayOutputStream que faz upload ao fechar
        return new RemoteOutputStream(relativePath);
    }

    @Override
    public InputStream getInputStream(String relativePath) {
        byte[] content = fileStorageClient.download(relativePath);
        return new ByteArrayInputStream(content);
    }

    @Override
    public void save(String relativePath, byte[] content) {
        try {
            String contentType = detectContentType(extractFilename(relativePath));
            fileStorageClient.uploadBytes(content, relativePath, contentType);
            log.debug("Arquivo enviado ao file-storage-service: {} ({} bytes)", relativePath, content.length);
        } catch (Exception e) {
            log.error("Erro ao salvar arquivo via file-storage-service {}: {}", relativePath, e.getMessage());
            throw new StorageException("Erro ao salvar arquivo: " + relativePath, e);
        }
    }

    @Override
    public void save(String relativePath, InputStream inputStream) {
        try {
            byte[] content = inputStream.readAllBytes();
            save(relativePath, content);
        } catch (IOException e) {
            throw new StorageException("Erro ao ler stream para upload: " + relativePath, e);
        }
    }

    @Override
    public byte[] load(String relativePath) {
        try {
            return fileStorageClient.download(relativePath);
        } catch (Exception e) {
            log.error("Erro ao carregar arquivo do file-storage-service {}: {}", relativePath, e.getMessage());
            throw new StorageException("Erro ao carregar arquivo: " + relativePath, e);
        }
    }

    @Override
    public InputStream loadAsStream(String relativePath) {
        return new ByteArrayInputStream(load(relativePath));
    }

    @Override
    public boolean delete(String relativePath) {
        try {
            Map<String, Object> result = fileStorageClient.delete(relativePath);
            return Boolean.TRUE.equals(result.get("deleted"));
        } catch (Exception e) {
            log.error("Erro ao deletar arquivo {}: {}", relativePath, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(String relativePath) {
        try {
            Map<String, Object> result = fileStorageClient.exists(relativePath);
            return Boolean.TRUE.equals(result.get("exists"));
        } catch (Exception e) {
            log.error("Erro ao verificar existência {}: {}", relativePath, e.getMessage());
            return false;
        }
    }

    @Override
    public Path getAbsolutePath(String relativePath) {
        // No modo remoto, retorna path virtual
        return Paths.get("remote-storage", relativePath);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> listFiles(String relativeDir) {
        try {
            Map<String, Object> result = fileStorageClient.list(relativeDir);
            Object files = result.get("files");
            if (files instanceof List) {
                return (List<String>) files;
            }
            return List.of();
        } catch (Exception e) {
            log.error("Erro ao listar arquivos {}: {}", relativeDir, e.getMessage());
            return List.of();
        }
    }

    @Override
    public long getFileSize(String relativePath) {
        try {
            Map<String, Object> result = fileStorageClient.info(relativePath);
            Object size = result.get("size");
            if (size instanceof Number) {
                return ((Number) size).longValue();
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public void createDirectory(String relativeDir) {
        // No MinIO/S3, diretórios são virtuais. Nada a fazer.
        log.debug("createDirectory no modo remoto é no-op: {}", relativeDir);
    }

    @Override
    public void move(String sourceRelativePath, String targetRelativePath) {
        fileStorageClient.move(Map.of("source", sourceRelativePath, "target", targetRelativePath));
    }

    @Override
    public void copy(String sourceRelativePath, String targetRelativePath) {
        fileStorageClient.copy(Map.of("source", sourceRelativePath, "target", targetRelativePath));
    }

    // ===== Utilitários =====

    private String extractFilename(String path) {
        if (path == null) return "file";
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private String detectContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return "application/octet-stream";
    }

    /**
     * OutputStream que faz upload ao file-storage-service quando fechado.
     */
    private class RemoteOutputStream extends ByteArrayOutputStream {
        private final String relativePath;

        RemoteOutputStream(String relativePath) {
            this.relativePath = relativePath;
        }

        @Override
        public void close() throws IOException {
            super.close();
            save(relativePath, toByteArray());
        }
    }
}
