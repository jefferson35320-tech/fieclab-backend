package br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Categoria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductDTO(
        String id,
        @NotBlank String name,
        @NotNull @Positive BigDecimal price,
        String description,
        Boolean ativo,
        LocalDate dataFabricacao,
        LocalDate validade,
        @URL String fispq,
        String imagem,
        @NotNull Categoria categoria,
        @Valid EstoqueDTO estoque
) {
}