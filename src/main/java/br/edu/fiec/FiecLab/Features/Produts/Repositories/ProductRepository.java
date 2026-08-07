package br.edu.fiec.FiecLab.Features.Produts.Repositories;


import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, ProductRepositoryCustom {
}
