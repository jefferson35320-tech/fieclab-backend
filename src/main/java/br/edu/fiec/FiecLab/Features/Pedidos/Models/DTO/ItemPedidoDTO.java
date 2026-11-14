package br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO;

import java.math.BigDecimal;

public record ItemPedidoDTO(
        String produtoId,
        String nome,
        int quantidade,
        BigDecimal precoUnitario
) {
}
