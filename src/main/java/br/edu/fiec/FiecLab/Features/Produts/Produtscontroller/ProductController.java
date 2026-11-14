package br.edu.fiec.FiecLab.Features.Produts.Produtscontroller;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO.ProductDTO;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Categoria;
import br.edu.fiec.FiecLab.Features.Produts.Services.ProductService;
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

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ProductDTO>> uploadCsv(@RequestParam("file") MultipartFile file) {
        List<ProductDTO> createdProducts = service.parseAndSaveCsv(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProducts);
    }
}