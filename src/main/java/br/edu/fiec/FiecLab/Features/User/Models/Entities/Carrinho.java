package br.edu.fiec.FiecLab.Features.User.Models.Entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Carrinho {
    private List<ItemCarrinho> itens = new ArrayList<>();
}