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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public List<ProductDTO> parseAndSaveCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo CSV enviado está vazio.");
        }

        List<Product> productsToSave = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isHeader = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Pula a primeira linha se for o cabeçalho do CSV
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    // Separa os valores por vírgula (ajuste para ";" se o seu CSV usar ponto e vírgula)
                    // O limite -1 mantém colunas vazias no final da linha
                    String[] data = line.split(",", -1);

                    // Colunas: name, price, description, imageUrl, type, batch, mfgDate, expDate, supplierId
                    String name = data[0].trim();
                    Double price = Double.parseDouble(data[1].trim());
                    String description = data.length > 2 && !data[2].isBlank() ? data[2].trim() : null;
                    String imageUrl = data.length > 3 && !data[3].isBlank() ? data[3].trim() : null;
                    ProductType type = ProductType.valueOf(data[4].trim().toUpperCase());
                    Integer batch = data.length > 5 && !data[5].isBlank() ? Integer.parseInt(data[5].trim()) : null;
                    LocalDate mfgDate = data.length > 6 && !data[6].isBlank() ? LocalDate.parse(data[6].trim()) : null;
                    LocalDate expDate = data.length > 7 && !data[7].isBlank() ? LocalDate.parse(data[7].trim()) : null;
                    Integer supplierId = data.length > 8 && !data[8].isBlank() ? Integer.parseInt(data[8].trim()) : null;

                    Product product = Product.builder()
                            .name(name)
                            .price(price)
                            .description(description)
                            .imageUrl(imageUrl)
                            .type(type)
                            .batch(batch)
                            .mfgDate(mfgDate)
                            .expDate(expDate)
                            .supplierId(supplierId)
                            .build();

                    productsToSave.add(product);

                } catch (RuntimeException e) {
                    throw new IllegalArgumentException(
                            "Erro de formatação na linha " + lineNumber + " do CSV: " + e.getMessage(), e);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar o arquivo CSV: " + e.getMessage(), e);
        }

        List<Product> savedProducts = repository.saveAll(productsToSave);
        return savedProducts.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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
            // Carrega a imagem na memória apenas UMA vez
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new IllegalArgumentException("O arquivo enviado não é uma imagem válida.");
            }

            // Gera um nome base único para as imagens
            String baseFileName = UUID.randomUUID() + ".jpg";

            // Caminhos de saída para o disco local
            Path mainImagePath = this.uploadDir.resolve(baseFileName);
            Path thumbImagePath = this.uploadDir.resolve("thumb_" + baseFileName);

            // 1. Gera e salva a imagem padrão (600x600 em formato JPG)
            Thumbnails.of(originalImage)
                    .size(600, 600)
                    .outputFormat("jpg")
                    .toFile(mainImagePath.toFile());

            // 2. Gera e salva o thumbnail (150x150 em formato JPG)
            Thumbnails.of(originalImage)
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