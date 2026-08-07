package br.edu.fiec.FiecLab.Features.Produts.Produtscontroller;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO.ProductDTO;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.ProductType;
import br.edu.fiec.FiecLab.Features.Produts.Services.ProductService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<ProductDTO>> findAll(
            @ParameterObject @PageableDefault(size = 10, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductDTO> create(@RequestBody ProductDTO dto) {
        ProductDTO created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> update(@PathVariable UUID id, @RequestBody ProductDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ProductDTO>> searchDynamic(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ProductType type,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String expDateBefore,
            @ParameterObject @PageableDefault(size = 10, sort = "name") Pageable pageable) {

        LocalDate expDate = (expDateBefore != null) ? LocalDate.parse(expDateBefore) : null;

        Page<ProductDTO> results = service.searchDynamic(name, type, minPrice, maxPrice, expDate, pageable);
        return ResponseEntity.ok(results);
    }
}