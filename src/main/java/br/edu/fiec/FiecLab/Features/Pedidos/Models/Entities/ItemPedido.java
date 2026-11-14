package br.edu.fiec.FiecLab.Features.Pedidos.Models.Entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

/** Subdocumento embutido em "pedidos.itens". Guarda uma cópia do nome e do preço no momento da compra. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedido {

    @Field(name = "produto_id", targetType = FieldType.OBJECT_ID)
    private String produtoId;

    private String nome;

    private int quantidade;

    @Field(name = "preco_unitario", targetType = FieldType.DECIMAL128)
    private BigDecimal precoUnitario;
}
