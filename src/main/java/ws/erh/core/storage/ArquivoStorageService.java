package ws.erh.core.storage;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface abstrata para serviço de armazenamento de arquivos.
 * Permite implementações para storage local, S3, Azure Blob, etc.
 */
public interface ArquivoStorageService {

    /**
     * Obtém um OutputStream para escrita no caminho especificado.
     * Cria diretórios automaticamente se não existirem.
     *
     * @param relativePath Caminho relativo ao diretório base
     * @return OutputStream para escrita
     */
    OutputStream getOutputStream(String relativePath);

    /**
     * Obtém um InputStream para leitura do arquivo especificado.
     *
     * @param relativePath Caminho relativo ao diretório base
     * @return InputStream para leitura
     */
    InputStream getInputStream(String relativePath);

    /**
     * Salva bytes diretamente no arquivo.
     *
     * @param relativePath Caminho relativo ao diretório base
     * @param content Conteúdo a ser salvo
     */
    void save(String relativePath, byte[] content);

    /**
     * Salva stream no arquivo.
     *
     * @param relativePath Caminho relativo ao diretório base
     * @param inputStream Stream de entrada
     */
    void save(String relativePath, InputStream inputStream);

    /**
     * Lê o conteúdo do arquivo como bytes.
     *
     * @param relativePath Caminho relativo ao diretório base
     * @return Conteúdo do arquivo
     */
    byte[] load(String relativePath);

    /**
     * Lê o conteúdo do arquivo como InputStream.
     *
     * @param relativePath Caminho relativo ao diretório base
     * @return InputStream do arquivo
     */
    InputStream loadAsStream(String relativePath);

    /**
     * Deleta o arquivo.
     *
     * @param relativePath Caminho relativo ao diretório base
     * @return true se deletado com sucesso
     */
    boolean delete(String relativePath);

    /**
     * Verifica se o arquivo existe.
     *
     * @param relativePath Caminho relativo ao diretório base
     * @return true se o arquivo existe
     */
    boolean exists(String relativePath);

    /**
     * Obtém o caminho absoluto do arquivo.
     *
     * @param relativePath Caminho relativo ao diretório base
     * @return Path absoluto
     */
    Path getAbsolutePath(String relativePath);

    /**
     * Lista arquivos em um diretório.
     *
     * @param relativeDir Diretório relativo ao diretório base
     * @return Lista de nomes de arquivos
     */
    List<String> listFiles(String relativeDir);

    /**
     * Obtém o tamanho do arquivo em bytes.
     *
     * @param relativePath Caminho relativo ao diretório base
     * @return Tamanho em bytes, ou -1 se não existir
     */
    long getFileSize(String relativePath);

    /**
     * Cria um diretório.
     *
     * @param relativeDir Diretório relativo ao diretório base
     */
    void createDirectory(String relativeDir);

    /**
     * Move um arquivo.
     *
     * @param sourceRelativePath Caminho de origem
     * @param targetRelativePath Caminho de destino
     */
    void move(String sourceRelativePath, String targetRelativePath);

    /**
     * Copia um arquivo.
     *
     * @param sourceRelativePath Caminho de origem
     * @param targetRelativePath Caminho de destino
     */
    void copy(String sourceRelativePath, String targetRelativePath);
}
