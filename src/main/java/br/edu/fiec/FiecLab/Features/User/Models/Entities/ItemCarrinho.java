package br.edu.fiec.FiecLab.Features.User.Models.Entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemCarrinho {

    @Field(name = "produto_id", targetType = FieldType.OBJECT_ID)
    private String produtoId;

    private int quantidade;

    @Field(name = "preco_unitario", targetType = FieldType.DECIMAL128)
    private BigDecimal precoUnitario;
}