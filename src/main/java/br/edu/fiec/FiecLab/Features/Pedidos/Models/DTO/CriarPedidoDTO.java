package br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO;

import jakarta.validation.Valid;

import java.util.List;

public record CriarPedidoDTO(
        @Valid List<ItemPedidoRequestDTO> itens
) {
}
