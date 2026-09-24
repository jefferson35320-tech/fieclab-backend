package br.edu.fiec.FiecLab.Features.Produts.Services;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO.ProductDTO;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Product;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.ProductType;
import br.edu.fiec.FiecLab.Features.Produts.Repositories.ProductRepository;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final Path uploadDir = Paths.get("uploads");

    public ProductService(ProductRepository repository) {
        this.repository = repository;
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar o diretório para upload de arquivos", e);
        }
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
        product.setId(null);
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

    public ProductDTO uploadImage(UUID id, MultipartFile file) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + id));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo enviado está vazio.");
        }

        try {
            // Gera um nome base único para as imagens
            String baseFileName = UUID.randomUUID() + ".jpg";

            // Caminhos de saída para o disco local
            Path mainImagePath = this.uploadDir.resolve(baseFileName);
            Path thumbImagePath = this.uploadDir.resolve("thumb_" + baseFileName);

            // 1. Gera e salva a imagem padrão (600x600 em formato JPG)
            Thumbnails.of(file.getInputStream())
                    .size(600, 600)
                    .outputFormat("jpg")
                    .toFile(mainImagePath.toFile());

            // 2. Gera e salva o thumbnail (150x150 em formato JPG)
            Thumbnails.of(file.getInputStream())
                    .size(150, 150)
                    .outputFormat("jpg")
                    .toFile(thumbImagePath.toFile());

            // Salva apenas o nome base da imagem no atributo imageUrl do produto
            product.setImageUrl(baseFileName);

            return toDTO(repository.save(product));

        } catch (IOException ex) {
            throw new RuntimeException("Falha ao processar e armazenar as imagens do produto: " + ex.getMessage(), ex);
        }
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