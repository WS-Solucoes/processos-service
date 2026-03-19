package ws.erh.core.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configurações para o serviço de armazenamento de arquivos.
 * Suporta LOCAL (sistema de arquivos), REMOTE (file-storage-service via Feign).
 *
 * Mapeamento YAML:
 *   ws.erh.storage.type     → tipo de storage (LOCAL, REMOTE)
 *   ws.erh.storage.local.*  → configurações do storage local
 */
@Configuration
@ConfigurationProperties(prefix = "ws.erh.storage")
public class StorageProperties {

    /**
     * Tipo de storage: LOCAL (dev), REMOTE (Docker/produção via file-storage-service)
     */
    private StorageType type = StorageType.LOCAL;

    /**
     * Configurações específicas para storage local
     */
    private Local local = new Local();

    public StorageType getType() {
        return type;
    }

    public void setType(StorageType type) {
        this.type = type;
    }

    // Alias para compatibilidade com código legado
    public StorageType getTipo() {
        return type;
    }

    public void setTipo(StorageType tipo) {
        this.type = tipo;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public enum StorageType {
        LOCAL,
        REMOTE
    }

    public static class Local {
        /**
         * Caminho base para armazenamento de arquivos.
         * Padrão: ${user.home}/ws-erh-files
         */
        private String basePath = System.getProperty("user.home") + "/ws-erh-files";

        /**
         * Subdiretório para exportações
         */
        private String exportPath = "exports";

        /**
         * Subdiretório para importações
         */
        private String importPath = "imports";

        /**
         * Subdiretório para arquivos temporários
         */
        private String tempPath = "temp";

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }

        public String getExportPath() {
            return exportPath;
        }

        public void setExportPath(String exportPath) {
            this.exportPath = exportPath;
        }

        public String getImportPath() {
            return importPath;
        }

        public void setImportPath(String importPath) {
            this.importPath = importPath;
        }

        public String getTempPath() {
            return tempPath;
        }

        public void setTempPath(String tempPath) {
            this.tempPath = tempPath;
        }

        public String getFullExportPath() {
            return basePath + "/" + exportPath;
        }

        public String getFullImportPath() {
            return basePath + "/" + importPath;
        }

        public String getFullTempPath() {
            return basePath + "/" + tempPath;
        }
    }
}
