package br.edu.fiec.FiecLab.Features.Produts.Services;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO.EstoqueDTO;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO.ProductDTO;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.CategoriaProduto;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Estoque;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Product;

import br.edu.fiec.FiecLab.Features.Produts.Repositories.ProductRepository;
import net.coobird.thumbnailator.Thumbnails;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Categoria;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final S3StorageService storage;

    public ProductService(ProductRepository repository, S3StorageService storage) {
        this.repository = repository;
        this.storage = storage;
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

                    String name = data[0].trim();
                    BigDecimal price = new BigDecimal(data[1].trim());
                    String description = data.length > 2 && !data[2].isBlank() ? data[2].trim() : null;
                    String imagem = data.length > 3 && !data[3].isBlank() ? data[3].trim() : null;
                    Categoria categoria = Categoria.valueOf(data[4].trim().toUpperCase());
                    LocalDate dataFabricacao = data.length > 5 && !data[5].isBlank() ? LocalDate.parse(data[5].trim()) : null;
                    LocalDate validade = data.length > 6 && !data[6].isBlank() ? LocalDate.parse(data[6].trim()) : null;
                    String fispq = data.length > 7 && !data[7].isBlank() ? data[7].trim() : null;
                    int quantidade = data.length > 8 && !data[8].isBlank() ? Integer.parseInt(data[8].trim()) : 0;
                    int quantidadeMinima = data.length > 9 && !data[9].isBlank() ? Integer.parseInt(data[9].trim()) : 0;

                    Product product = new Product();
                    product.setName(name);
                    product.setPrice(price);
                    product.setDescription(description);
                    product.setImagem(imagem);
                    product.setCategoria(new CategoriaProduto(categoria));
                    product.setDataFabricacao(dataFabricacao);
                    product.setValidade(validade);
                    product.setFispq(fispq);
                    product.setEstoque(new Estoque(quantidade, quantidadeMinima, LocalDateTime.now()));

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

    public ProductDTO findById(String id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + id));
        return toDTO(product);
    }

    public ProductDTO create(ProductDTO dto) {
        Product product = new Product();
        applyDto(product, dto);
        return toDTO(repository.save(product));
    }

    public ProductDTO update(String id, ProductDTO dto) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + id));
        applyDto(product, dto);
        return toDTO(repository.save(product));
    }

    public void delete(String id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Produto não encontrado: " + id);
        }
        repository.deleteById(id);
    }

    public Page<ProductDTO> searchDynamic(
            String name,
            Categoria categoria,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            LocalDate expDateBefore,
            Pageable pageable) {

        return repository.findProductsByCriteria(name, categoria, minPrice, maxPrice, expDateBefore, pageable)
                .map(this::toDTO);
    }

    public ProductDTO uploadImage(String id, MultipartFile file) {
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

            // 1. Imagem padrão (600x600 JPG) -> S3
            storage.upload(baseFileName, toJpegBytes(originalImage, 600), "image/jpeg");

            // 2. Thumbnail (150x150 JPG) -> S3
            storage.upload("thumb_" + baseFileName, toJpegBytes(originalImage, 150), "image/jpeg");

            // Salva apenas o nome base da imagem no produto
            product.setImagem(baseFileName);

            return toDTO(repository.save(product));

        } catch (IOException ex) {
            throw new RuntimeException("Falha ao processar e armazenar as imagens do produto: " + ex.getMessage(), ex);
        }
    }

    private byte[] toJpegBytes(BufferedImage image, int size) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(image)
                .size(size, size)
                .outputFormat("jpg")
                .toOutputStream(out);
        return out.toByteArray();
    }

    private ProductDTO toDTO(Product p) {
        Estoque e = p.getEstoque();
        EstoqueDTO estoqueDTO = e == null ? null
                : new EstoqueDTO(e.getQuantidade(), e.getQuantidadeMinima(), e.getDataAtualizacao());

        return new ProductDTO(
                p.getId(),
                p.getName(),
                p.getPrice(),
                p.getDescription(),
                p.isAtivo(),
                p.getDataFabricacao(),
                p.getValidade(),
                p.getFispq(),
                p.getImagem(),
                p.getCategoria() == null ? null : p.getCategoria().getNome(),
                estoqueDTO
        );
    }

    private void applyDto(Product p, ProductDTO dto) {
        p.setName(dto.name());
        p.setPrice(dto.price());
        p.setDescription(dto.description());
        if (dto.ativo() != null) {
            p.setAtivo(dto.ativo());
        }
        p.setDataFabricacao(dto.dataFabricacao());
        p.setValidade(dto.validade());
        p.setFispq(dto.fispq());
        p.setImagem(dto.imagem());
        p.setCategoria(new CategoriaProduto(dto.categoria()));

        if (dto.estoque() != null) {
            Estoque e = p.getEstoque() != null ? p.getEstoque() : new Estoque();
            e.setQuantidade(dto.estoque().quantidade());
            e.setQuantidadeMinima(dto.estoque().quantidadeMinima());
            e.setDataAtualizacao(LocalDateTime.now());
            p.setEstoque(e);
        }
    }
    public String getImageUrl(String id, boolean thumb) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado: " + id));

        String imagem = product.getImagem();
        if (imagem == null || imagem.isBlank()) {
            throw new NoSuchElementException("Produto sem imagem: " + id);
        }

        // produtos vindos do CSV podem ter uma URL externa no campo imagem
        if (imagem.startsWith("http")) {
            return imagem;
        }

        String key = thumb ? "thumb_" + imagem : imagem;
        return storage.presignedUrl(key, Duration.ofMinutes(30));
    }
}