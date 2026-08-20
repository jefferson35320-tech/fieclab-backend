package br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tb_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Double preco;

    private String descricao;

    private String imagem_em_texto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType tipo_de_Produto;

    private Integer estoque;
}