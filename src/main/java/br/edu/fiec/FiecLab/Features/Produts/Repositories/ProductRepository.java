package br.edu.fiec.FiecLab.Features.Produts.Repositories;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends MongoRepository<Product, String>, ProductRepositoryCustom {
}