package br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.DTO;

import jakarta.validation.constraints.Min;

public record BaixaEstoqueDTO(@Min(1) int quantidade) {
}