package br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO;

import jakarta.validation.Valid;

import java.util.List;

/** Se "itens" vier vazio ou o corpo for omitido, o pedido é criado a partir do carrinho do usuário. */
public record CriarPedidoDTO(
        @Valid List<ItemPedidoRequestDTO> itens
) {
}
