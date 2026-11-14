package br.edu.fiec.FiecLab.Features.Pedidos.Models.Entities;

public enum StatusPedido {
    PENDENTE,
    PAGO,
    ENVIADO,
    ENTREGUE,
    CANCELADO;

    /** Transições permitidas do fluxo do pedido. */
    public boolean podeIrPara(StatusPedido novo) {
        return switch (this) {
            case PENDENTE -> novo == PAGO || novo == CANCELADO;
            case PAGO -> novo == ENVIADO || novo == CANCELADO;
            case ENVIADO -> novo == ENTREGUE;
            case ENTREGUE, CANCELADO -> false;
        };
    }
}
