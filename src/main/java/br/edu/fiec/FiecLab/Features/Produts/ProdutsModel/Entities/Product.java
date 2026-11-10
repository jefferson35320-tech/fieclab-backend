package br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "tb_products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    private String id;

    private String name;

    private Double price;

    private String description;

    private String imageUrl;

    private ProductType type;

    private Integer batch;

    private LocalDate mfgDate;

    private LocalDate expDate;

    private Integer supplierId;
}