package ws.erh.core.storage;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Feign Client para comunicação com o file-storage-service (porta 9085).
 * Usa Eureka para descoberta de serviço.
 */
@FeignClient(
        name = "file-storage-service",
        url = "${file-storage.service.url:}",
        fallbackFactory = FileStorageClientFallbackFactory.class
)
public interface FileStorageClient {

    @PostMapping(value = "/api/v1/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, Object> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("storagePath") String storagePath
    );

    @PostMapping(value = "/api/v1/files/bytes", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    Map<String, Object> uploadBytes(
            @RequestBody byte[] content,
            @RequestParam("storagePath") String storagePath,
            @RequestParam("contentType") String contentType
    );

    @GetMapping("/api/v1/files/download/bytes")
    byte[] download(@RequestParam("path") String storagePath);

    @DeleteMapping("/api/v1/files")
    Map<String, Object> delete(@RequestParam("path") String storagePath);

    @GetMapping("/api/v1/files/exists")
    Map<String, Object> exists(@RequestParam("path") String storagePath);

    @GetMapping("/api/v1/files/info")
    Map<String, Object> info(@RequestParam("path") String storagePath);

    @GetMapping("/api/v1/files/list")
    Map<String, Object> list(@RequestParam("prefix") String prefix);

    @PostMapping("/api/v1/files/copy")
    Map<String, Object> copy(@RequestBody Map<String, String> body);

    @PostMapping("/api/v1/files/move")
    Map<String, Object> move(@RequestBody Map<String, String> body);
}
