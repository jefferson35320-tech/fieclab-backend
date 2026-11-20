package br.edu.fiec.FiecLab.config;

import br.edu.fiec.FiecLab.Features.Produts.Services.S3StorageService;
import br.edu.fiec.FiecLab.config.mcp.McpSupport;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Base64;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/images")
public class FileController {

    private final S3StorageService storage;

    public FileController(S3StorageService storage) {
        this.storage = storage;
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<byte[]> getImage(@PathVariable String fileName) {
        try {
            var obj = storage.download(fileName);

            String contentType = obj.response().contentType() != null
                    ? obj.response().contentType()
                    : MediaType.IMAGE_JPEG_VALUE;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
                    .body(obj.asByteArray());

        } catch (NoSuchKeyException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ------------------------------------------------------------------
    // Ferramenta MCP (exposta para agentes de IA em /mcp)
    // ------------------------------------------------------------------

    @McpTool(name = "imagem_baixar",
            description = "Baixa um arquivo de imagem do armazenamento (S3) pelo nome e devolve o conteúdo em Base64. "
                    + "Para obter a URL da imagem de um produto, prefira produto_url_imagem.",
            annotations = @McpTool.McpAnnotations(title = "Baixar imagem",
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public McpSupport.McpFile mcpGetImage(
            @McpToolParam(description = "Nome do arquivo (chave no S3), ex.: thumb_abc.jpg") String fileName) {
        try {
            var obj = storage.download(fileName);
            String contentType = obj.response().contentType() != null
                    ? obj.response().contentType()
                    : MediaType.IMAGE_JPEG_VALUE;
            return new McpSupport.McpFile(fileName, contentType,
                    Base64.getEncoder().encodeToString(obj.asByteArray()));
        } catch (NoSuchKeyException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Imagem não encontrada: " + fileName);
        }
    }
}