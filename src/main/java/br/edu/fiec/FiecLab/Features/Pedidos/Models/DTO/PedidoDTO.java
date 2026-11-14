package br.edu.fiec.FiecLab.Features.Pedidos.Models.DTO;

import br.edu.fiec.FiecLab.Features.Pedidos.Models.Entities.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoDTO(
        String id,
        String usuarioId,
        LocalDateTime dataPedido,
        BigDecimal valorTotal,
        StatusPedido status,
        List<ItemPedidoDTO> itens
) {
}
