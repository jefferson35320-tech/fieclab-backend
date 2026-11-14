package br.edu.fiec.FiecLab.Features.Pedidos.Models.Entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "pedidos")
public class Pedido {

    @MongoId(FieldType.OBJECT_ID)
    private String id;

    @Indexed
    @Field(name = "usuario_id", targetType = FieldType.OBJECT_ID)
    private String usuarioId;

    @Field("data_pedido")
    private LocalDateTime dataPedido;

    @Field(name = "valor_total", targetType = FieldType.DECIMAL128)
    private BigDecimal valorTotal;

    private StatusPedido status = StatusPedido.PENDENTE;

    private List<ItemPedido> itens = new ArrayList<>();
}
