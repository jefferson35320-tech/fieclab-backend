package br.edu.fiec.FiecLab.Features.Produts.Produtscontroller;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO.AvaliarDTO;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO.BaixaEstoqueDTO;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO.ProductDTO;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Categoria;
import br.edu.fiec.FiecLab.Features.Produts.Services.ProductService;
import br.edu.fiec.FiecLab.config.mcp.McpSupport;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.data.domain.Sort;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Map<String, String>> getImage(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean thumb) {
        return ResponseEntity.ok(Map.of("url", service.getImageUrl(id, thumb)));
    }

    @GetMapping
    public ResponseEntity<Page<ProductDTO>> findAll(
            @ParameterObject @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductDTO> create(@Valid @RequestBody ProductDTO dto) {
        ProductDTO created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/avaliacoes")
    public ResponseEntity<ProductDTO> rate(
            @PathVariable String id,
            @Valid @RequestBody AvaliarDTO dto) {
        return ResponseEntity.ok(service.rate(id, dto.estrelas()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> update(@PathVariable String id, @Valid @RequestBody ProductDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductDTO>> searchDynamic(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Categoria categoria,      // e ra ProductType type
            @RequestParam(required = false) BigDecimal minPrice,      // era Double
            @RequestParam(required = false) BigDecimal maxPrice,      // era Double
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expDateBefore,
            @ParameterObject @PageableDefault(size = 10, sort = "name") Pageable pageable) {

        return ResponseEntity.ok(
                service.searchDynamic(name, categoria, minPrice, maxPrice, expDateBefore, pageable));
    }
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDTO> uploadImage(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {

        ProductDTO updatedProduct = service.uploadImage(id, file);
        return ResponseEntity.ok(updatedProduct);
    }

    @PatchMapping("/{id}/estoque")
    public ResponseEntity<ProductDTO> baixarEstoque(
            @PathVariable String id,
            @Valid @RequestBody BaixaEstoqueDTO dto) {
        return ResponseEntity.ok(service.baixarEstoque(id, dto.quantidade()));
    }

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ProductDTO>> uploadCsv(@RequestParam("file") MultipartFile file) {
        List<ProductDTO> createdProducts = service.parseAndSaveCsv(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProducts);
    }

    // ------------------------------------------------------------------
    // Ferramentas MCP (expostas para agentes de IA em /mcp)
    // ------------------------------------------------------------------

    private static final Sort SORT_NAME = Sort.by("name");

    @McpTool(name = "produto_url_imagem",
            description = "Retorna a URL (temporária) da imagem de um produto, ou da miniatura se thumb=true.",
            annotations = @McpTool.McpAnnotations(title = "URL da imagem do produto",
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public Map<String, String> mcpGetImage(
            @McpToolParam(description = "Id do produto") String id,
            @McpToolParam(description = "true para a miniatura; padrão false", required = false) Boolean thumb) {
        return Map.of("url", service.getImageUrl(id, Boolean.TRUE.equals(thumb)));
    }

    @McpTool(name = "produto_listar",
            description = "Lista os produtos do catálogo ordenados por nome (paginado).",
            annotations = @McpTool.McpAnnotations(title = "Listar produtos",
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public McpSupport.McpPage<ProductDTO> mcpFindAll(
            @McpToolParam(description = "Número da página, começando em 0 (padrão 0)", required = false) Integer page,
            @McpToolParam(description = "Itens por página (padrão 10, máximo 100)", required = false) Integer size) {
        return McpSupport.McpPage.of(service.findAll(McpSupport.pageable(page, size, SORT_NAME)));
    }

    @McpTool(name = "produto_buscar",
            description = "Busca um produto pelo id, incluindo estoque e avaliações.",
            annotations = @McpTool.McpAnnotations(title = "Buscar produto",
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public ProductDTO mcpFindById(@McpToolParam(description = "Id do produto") String id) {
        return service.findById(id);
    }

    @McpTool(name = "produto_pesquisar",
            description = "Pesquisa produtos com filtros opcionais combináveis: nome (parcial), categoria, "
                    + "faixa de preço e validade até uma data. Resultado paginado e ordenado por nome.",
            annotations = @McpTool.McpAnnotations(title = "Pesquisar produtos",
                    readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public McpSupport.McpPage<ProductDTO> mcpSearchDynamic(
            @McpToolParam(description = "Parte do nome do produto", required = false) String name,
            @McpToolParam(description = "Categoria: REAGENTE, VIDRARIA, EQUIPAMENTO, EPI ou OUTROS",
                    required = false) Categoria categoria,
            @McpToolParam(description = "Preço mínimo", required = false) BigDecimal minPrice,
            @McpToolParam(description = "Preço máximo", required = false) BigDecimal maxPrice,
            @McpToolParam(description = "Somente produtos com validade até esta data, inclusive (ISO yyyy-MM-dd)",
                    required = false) LocalDate expDateBefore,
            @McpToolParam(description = "Número da página, começando em 0 (padrão 0)", required = false) Integer page,
            @McpToolParam(description = "Itens por página (padrão 10, máximo 100)", required = false) Integer size) {
        return McpSupport.McpPage.of(service.searchDynamic(name, categoria, minPrice, maxPrice, expDateBefore,
                McpSupport.pageable(page, size, SORT_NAME)));
    }

    @McpTool(name = "produto_criar",
            description = "Cadastra um novo produto. Campos obrigatórios: name, price (> 0) e categoria. "
                    + "Ignore id, imagem e os campos de avaliação (são gerados pelo sistema).",
            annotations = @McpTool.McpAnnotations(title = "Criar produto",
                    readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    public ProductDTO mcpCreate(@McpToolParam(description = "Dados do produto") ProductDTO produto) {
        return service.create(McpSupport.validate(produto));
    }

    @McpTool(name = "produto_avaliar",
            description = "Registra uma avaliação de 0.5 a 5.0 estrelas para um produto e retorna o produto atualizado.",
            annotations = @McpTool.McpAnnotations(title = "Avaliar produto",
                    readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    public ProductDTO mcpRate(
            @McpToolParam(description = "Id do produto") String id,
            @McpToolParam(description = "Nota entre 0.5 e 5.0") double estrelas) {
        return service.rate(id, McpSupport.validate(new AvaliarDTO(estrelas)).estrelas());
    }

    @McpTool(name = "produto_atualizar",
            description = "Substitui os dados de um produto existente (mesmas regras de produto_criar).",
            annotations = @McpTool.McpAnnotations(title = "Atualizar produto",
                    readOnlyHint = false, destructiveHint = true, idempotentHint = true, openWorldHint = false))
    public ProductDTO mcpUpdate(
            @McpToolParam(description = "Id do produto") String id,
            @McpToolParam(description = "Novos dados do produto") ProductDTO produto) {
        return service.update(id, McpSupport.validate(produto));
    }

    @McpTool(name = "produto_excluir",
            description = "Exclui permanentemente um produto. Confirme com o usuário antes de usar.",
            annotations = @McpTool.McpAnnotations(title = "Excluir produto",
                    readOnlyHint = false, destructiveHint = true, idempotentHint = true, openWorldHint = false))
    public String mcpDelete(@McpToolParam(description = "Id do produto") String id) {
        service.delete(id);
        return "Produto " + id + " excluído";
    }

    @McpTool(name = "produto_enviar_imagem",
            description = "Envia (ou substitui) a imagem de um produto. O arquivo deve ser enviado em Base64.",
            annotations = @McpTool.McpAnnotations(title = "Enviar imagem do produto",
                    readOnlyHint = false, destructiveHint = true, idempotentHint = true, openWorldHint = false))
    public ProductDTO mcpUploadImage(
            @McpToolParam(description = "Id do produto") String id,
            @McpToolParam(description = "Nome do arquivo, ex.: foto.jpg") String fileName,
            @McpToolParam(description = "MIME type, ex.: image/jpeg ou image/png") String contentType,
            @McpToolParam(description = "Conteúdo da imagem codificado em Base64") String base64Content) {
        return service.uploadImage(id, McpSupport.multipart(fileName, contentType, base64Content));
    }

    @McpTool(name = "produto_baixar_estoque",
            description = "Dá baixa (diminui) na quantidade em estoque de um produto.",
            annotations = @McpTool.McpAnnotations(title = "Baixar estoque",
                    readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    public ProductDTO mcpBaixarEstoque(
            @McpToolParam(description = "Id do produto") String id,
            @McpToolParam(description = "Quantidade a retirar (>= 1)") int quantidade) {
        return service.baixarEstoque(id, McpSupport.validate(new BaixaEstoqueDTO(quantidade)).quantidade());
    }

    @McpTool(name = "produto_importar_csv",
            description = "Importa produtos em lote a partir de um arquivo CSV (conteúdo em texto puro, "
                    + "no mesmo formato aceito por POST /api/products/csv). Retorna os produtos criados.",
            annotations = @McpTool.McpAnnotations(title = "Importar produtos via CSV",
                    readOnlyHint = false, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    public List<ProductDTO> mcpUploadCsv(
            @McpToolParam(description = "Conteúdo do CSV em texto (UTF-8), incluindo o cabeçalho") String csvContent) {
        String base64 = Base64.getEncoder().encodeToString(
                (csvContent == null ? "" : csvContent).getBytes(StandardCharsets.UTF_8));
        return service.parseAndSaveCsv(McpSupport.multipart("produtos.csv", "text/csv", base64));
    }
}