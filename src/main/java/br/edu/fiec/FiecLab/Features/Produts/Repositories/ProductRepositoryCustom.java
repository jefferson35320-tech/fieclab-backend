package br.edu.fiec.FiecLab.Features.Produts.Repositories;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Product;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ProductRepositoryCustom {
    Page<Product> findProductsByCriteria(
            String name,
            ProductType type,
            Double minPrice,
            Double maxPrice,
            LocalDate expDateBefore,
            Pageable pageable
    );
}