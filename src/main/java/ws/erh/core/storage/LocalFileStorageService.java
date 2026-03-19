package ws.erh.core.storage;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import ws.erh.core.storage.StorageProperties;

/**
 * Implementação de ArquivoStorageService para storage local em sistema de arquivos.
 * Utiliza configurações de StorageProperties para definir diretórios base.
 * Ativado quando ws.erh.storage.type=LOCAL (padrão para desenvolvimento).
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    prefix = "ws.erh.storage", name = "type", havingValue = "LOCAL", matchIfMissing = false
)
public class LocalFileStorageService implements ArquivoStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final StorageProperties storageProperties;
    private Path basePath;

    public LocalFileStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    public void init() {
        this.basePath = Paths.get(storageProperties.getLocal().getBasePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(basePath);
            Files.createDirectories(basePath.resolve(storageProperties.getLocal().getExportPath()));
            Files.createDirectories(basePath.resolve(storageProperties.getLocal().getImportPath()));
            Files.createDirectories(basePath.resolve(storageProperties.getLocal().getTempPath()));
            logger.info("Storage local inicializado em: {}", basePath);
        } catch (IOException e) {
            logger.error("Erro ao criar diretórios de storage: {}", e.getMessage());
            throw new StorageException("Não foi possível criar diretórios de storage", e);
        }
    }

    @Override
    public OutputStream getOutputStream(String relativePath) {
        try {
            Path targetPath = resolveAndValidate(relativePath);
            Files.createDirectories(targetPath.getParent());
            return new FileOutputStream(targetPath.toFile());
        } catch (IOException e) {
            logger.error("Erro ao criar OutputStream para {}: {}", relativePath, e.getMessage());
            throw new StorageException("Erro ao criar arquivo para escrita: " + relativePath, e);
        }
    }

    @Override
    public InputStream getInputStream(String relativePath) {
        try {
            Path sourcePath = resolveAndValidate(relativePath);
            if (!Files.exists(sourcePath)) {
                throw new StorageException("Arquivo não encontrado: " + relativePath);
            }
            return new FileInputStream(sourcePath.toFile());
        } catch (IOException e) {
            logger.error("Erro ao criar InputStream para {}: {}", relativePath, e.getMessage());
            throw new StorageException("Erro ao abrir arquivo para leitura: " + relativePath, e);
        }
    }

    @Override
    public void save(String relativePath, byte[] content) {
        try (OutputStream out = getOutputStream(relativePath)) {
            out.write(content);
            logger.debug("Arquivo salvo: {} ({} bytes)", relativePath, content.length);
        } catch (IOException e) {
            logger.error("Erro ao salvar arquivo {}: {}", relativePath, e.getMessage());
            throw new StorageException("Erro ao salvar arquivo: " + relativePath, e);
        }
    }

    @Override
    public void save(String relativePath, InputStream inputStream) {
        try {
            Path targetPath = resolveAndValidate(relativePath);
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("Arquivo salvo via stream: {}", relativePath);
        } catch (IOException e) {
            logger.error("Erro ao salvar stream em {}: {}", relativePath, e.getMessage());
            throw new StorageException("Erro ao salvar arquivo: " + relativePath, e);
        }
    }

    @Override
    public byte[] load(String relativePath) {
        try (InputStream in = getInputStream(relativePath);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        } catch (IOException e) {
            logger.error("Erro ao carregar arquivo {}: {}", relativePath, e.getMessage());
            throw new StorageException("Erro ao carregar arquivo: " + relativePath, e);
        }
    }

    @Override
    public InputStream loadAsStream(String relativePath) {
        return getInputStream(relativePath);
    }

    @Override
    public boolean delete(String relativePath) {
        try {
            Path targetPath = resolveAndValidate(relativePath);
            boolean deleted = Files.deleteIfExists(targetPath);
            if (deleted) {
                logger.debug("Arquivo deletado: {}", relativePath);
            }
            return deleted;
        } catch (IOException e) {
            logger.error("Erro ao deletar arquivo {}: {}", relativePath, e.getMessage());
            throw new StorageException("Erro ao deletar arquivo: " + relativePath, e);
        }
    }

    @Override
    public boolean exists(String relativePath) {
        Path targetPath = resolveAndValidate(relativePath);
        return Files.exists(targetPath);
    }

    @Override
    public Path getAbsolutePath(String relativePath) {
        return resolveAndValidate(relativePath);
    }

    @Override
    public List<String> listFiles(String relativeDir) {
        try {
            Path dirPath = resolveAndValidate(relativeDir);
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return Collections.emptyList();
            }
            try (Stream<Path> stream = Files.list(dirPath)) {
                return stream
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            logger.error("Erro ao listar arquivos em {}: {}", relativeDir, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public long getFileSize(String relativePath) {
        try {
            Path targetPath = resolveAndValidate(relativePath);
            if (!Files.exists(targetPath)) {
                return -1;
            }
            return Files.size(targetPath);
        } catch (IOException e) {
            logger.error("Erro ao obter tamanho do arquivo {}: {}", relativePath, e.getMessage());
            return -1;
        }
    }

    @Override
    public void createDirectory(String relativeDir) {
        try {
            Path dirPath = resolveAndValidate(relativeDir);
            Files.createDirectories(dirPath);
            logger.debug("Diretório criado: {}", relativeDir);
        } catch (IOException e) {
            logger.error("Erro ao criar diretório {}: {}", relativeDir, e.getMessage());
            throw new StorageException("Erro ao criar diretório: " + relativeDir, e);
        }
    }

    @Override
    public void move(String sourceRelativePath, String targetRelativePath) {
        try {
            Path sourcePath = resolveAndValidate(sourceRelativePath);
            Path targetPath = resolveAndValidate(targetRelativePath);
            Files.createDirectories(targetPath.getParent());
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("Arquivo movido de {} para {}", sourceRelativePath, targetRelativePath);
        } catch (IOException e) {
            logger.error("Erro ao mover arquivo de {} para {}: {}", sourceRelativePath, targetRelativePath, e.getMessage());
            throw new StorageException("Erro ao mover arquivo", e);
        }
    }

    @Override
    public void copy(String sourceRelativePath, String targetRelativePath) {
        try {
            Path sourcePath = resolveAndValidate(sourceRelativePath);
            Path targetPath = resolveAndValidate(targetRelativePath);
            Files.createDirectories(targetPath.getParent());
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("Arquivo copiado de {} para {}", sourceRelativePath, targetRelativePath);
        } catch (IOException e) {
            logger.error("Erro ao copiar arquivo de {} para {}: {}", sourceRelativePath, targetRelativePath, e.getMessage());
            throw new StorageException("Erro ao copiar arquivo", e);
        }
    }

    /**
     * Resolve o caminho relativo e valida que está dentro do diretório base.
     */
    private Path resolveAndValidate(String relativePath) {
        Path resolved = basePath.resolve(relativePath).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new StorageException("Acesso negado: caminho fora do diretório base");
        }
        return resolved;
    }

    // ============== MÉTODOS AUXILIARES PARA EXPORTAÇÃO/IMPORTAÇÃO ==============

    /**
     * Obtém caminho para arquivo de exportação.
     */
    public String getExportPath(String cnpj, String filename) {
        return storageProperties.getLocal().getExportPath() + "/" + cnpj + "/" + filename;
    }

    /**
     * Obtém caminho para arquivo de importação.
     */
    public String getImportPath(String cnpj, String filename) {
        return storageProperties.getLocal().getImportPath() + "/" + cnpj + "/" + filename;
    }

    /**
     * Obtém caminho para arquivo temporário.
     */
    public String getTempPath(String filename) {
        return storageProperties.getLocal().getTempPath() + "/" + filename;
    }
}
