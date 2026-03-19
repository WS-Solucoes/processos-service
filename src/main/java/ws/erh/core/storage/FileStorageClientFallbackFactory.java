package ws.erh.core.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Fallback factory para o FileStorageClient.
 * Loga erros quando o file-storage-service não está disponível.
 */
@Component
public class FileStorageClientFallbackFactory implements FallbackFactory<FileStorageClient> {

    private static final Logger log = LoggerFactory.getLogger(FileStorageClientFallbackFactory.class);

    @Override
    public FileStorageClient create(Throwable cause) {
        log.error("File Storage Service indisponível: {}", cause.getMessage());

        return new FileStorageClient() {
            @Override
            public Map<String, Object> upload(MultipartFile file, String storagePath) {
                throw new StorageException("File Storage Service indisponível para upload: " + cause.getMessage(), cause);
            }

            @Override
            public Map<String, Object> uploadBytes(byte[] content, String storagePath, String contentType) {
                throw new StorageException("File Storage Service indisponível para upload: " + cause.getMessage(), cause);
            }

            @Override
            public byte[] download(String storagePath) {
                throw new StorageException("File Storage Service indisponível para download: " + cause.getMessage(), cause);
            }

            @Override
            public Map<String, Object> delete(String storagePath) {
                throw new StorageException("File Storage Service indisponível para delete: " + cause.getMessage(), cause);
            }

            @Override
            public Map<String, Object> exists(String storagePath) {
                return Map.of("exists", false, "error", "service_unavailable");
            }

            @Override
            public Map<String, Object> info(String storagePath) {
                return Map.of("exists", false, "error", "service_unavailable");
            }

            @Override
            public Map<String, Object> list(String prefix) {
                return Map.of("files", List.of(), "error", "service_unavailable");
            }

            @Override
            public Map<String, Object> copy(Map<String, String> body) {
                throw new StorageException("File Storage Service indisponível para copy: " + cause.getMessage(), cause);
            }

            @Override
            public Map<String, Object> move(Map<String, String> body) {
                throw new StorageException("File Storage Service indisponível para move: " + cause.getMessage(), cause);
            }
        };
    }
}
