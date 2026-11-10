package br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.ProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProductDTO(
        String id,
        @NotBlank String name,
        @NotNull Double price,
        String description,
        String imageUrl,
        @NotNull ProductType type,
        Integer batch,
        LocalDate mfgDate,
        LocalDate expDate,
        Integer supplierId
) {}