package br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO;

import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

public record EstoqueDTO(
        @Min(0) int quantidade,
        @Min(0) int quantidadeMinima,
        LocalDateTime dataAtualizacao
) {
}