package br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record AvaliarDTO(
        @DecimalMin("0.5") @DecimalMax("5.0") double estrelas
) {
}