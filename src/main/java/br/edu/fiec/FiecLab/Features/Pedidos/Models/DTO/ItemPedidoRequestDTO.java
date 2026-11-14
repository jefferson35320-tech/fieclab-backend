package br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Só produto e quantidade: nome e preço sempre vêm do banco, nunca do cliente. */
public record ItemPedidoRequestDTO(
        @NotBlank String produtoId,
        @Min(1) int quantidade
) {
}
