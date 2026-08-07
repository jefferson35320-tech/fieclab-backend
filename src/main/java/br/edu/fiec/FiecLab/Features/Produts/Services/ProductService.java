package br.edu.fiec.FiecLab.Features.Produts.Services;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO.ProductDTO;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Product;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.ProductType;
import br.edu.fiec.FiecLab.Features.Produts.Repositories.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Page<ProductDTO> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDTO);
    }

    public ProductDTO findById(UUID id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + id));
        return toDTO(product);
    }

    public ProductDTO create(ProductDTO dto) {
        Product product = toEntity(dto);
        product.setId(null); // garante que é sempre uma criação, ignorando id vindo do body
        return toDTO(repository.save(product));
    }

    public ProductDTO update(UUID id, ProductDTO dto) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + id));

        product.setName(dto.name());
        product.setPrice(dto.price());
        product.setDescription(dto.description());
        product.setImageUrl(dto.imageUrl());
        product.setType(dto.type());
        product.setBatch(dto.batch());
        product.setMfgDate(dto.mfgDate());
        product.setExpDate(dto.expDate());
        product.setSupplierId(dto.supplierId());

        return toDTO(repository.save(product));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Produto não encontrado: " + id);
        }
        repository.deleteById(id);
    }

    public Page<ProductDTO> searchDynamic(
            String name,
            ProductType type,
            Double minPrice,
            Double maxPrice,
            LocalDate expDateBefore,
            Pageable pageable) {

        return repository.findProductsByCriteria(name, type, minPrice, maxPrice, expDateBefore, pageable)
                .map(this::toDTO);
    }

    private ProductDTO toDTO(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDescription(),
                product.getImageUrl(),
                product.getType(),
                product.getBatch(),
                product.getMfgDate(),
                product.getExpDate(),
                product.getSupplierId()
        );
    }

    private Product toEntity(ProductDTO dto) {
        return Product.builder()
                .id(dto.id())
                .name(dto.name())
                .price(dto.price())
                .description(dto.description())
                .imageUrl(dto.imageUrl())
                .type(dto.type())
                .batch(dto.batch())
                .mfgDate(dto.mfgDate())
                .expDate(dto.expDate())
                .supplierId(dto.supplierId())
                .build();
    }
}