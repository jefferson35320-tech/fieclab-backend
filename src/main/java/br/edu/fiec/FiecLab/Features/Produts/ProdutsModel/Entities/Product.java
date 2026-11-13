package br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "produtos")
public class Product {

    @MongoId(FieldType.OBJECT_ID)
    private String id;

    @Field("nome")
    private String name;

    @Field("descricao")
    private String description;

    @Field(name = "preco", targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    private boolean ativo = true;

    @Field("data_fabricacao")
    private LocalDate dataFabricacao;

    private LocalDate validade;

    private String fispq;   // link

    private String imagem;  // URL

    private CategoriaProduto categoria;

    private Estoque estoque = new Estoque();
}