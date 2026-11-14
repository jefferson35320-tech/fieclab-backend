package br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO;

import br.edu.fiec.FiecLab.Features.Pedidos.Models.Entities.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusPedidoDTO(
        @NotNull StatusPedido status
) {
}
