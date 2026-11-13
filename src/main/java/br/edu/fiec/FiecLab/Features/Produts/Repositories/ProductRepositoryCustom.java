package br.edu.fiec.FiecLab.Features.Produts.Repositories;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Categoria;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ProductRepositoryCustom {

    Page<Product> findProductsByCriteria(
            String name,
            Categoria categoria,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            LocalDate expDateBefore,
            Pageable pageable);
}