package br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO;


import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.ProductType;

import java.time.LocalDate;
import java.util.UUID;

public record ProductDTO(
        UUID id,
        String name,
        Double price,
        String description,
        String imageUrl,
        ProductType type,
        Integer batch,
        LocalDate mfgDate,
        LocalDate expDate,
        Integer supplierId
) {}