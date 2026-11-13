package br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Estoque {

    private int quantidade;

    @Field("quantidade_minima")
    private int quantidadeMinima;

    @Field("data_atualizacao")
    private LocalDateTime dataAtualizacao;
}