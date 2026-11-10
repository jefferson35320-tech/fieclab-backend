package br.edu.fiec.FiecLab.Features.Produts.Repositories;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Product;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.ProductType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public ProductRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<Product> findProductsByCriteria(
            String name,
            ProductType type,
            Double minPrice,
            Double maxPrice,
            LocalDate expDateBefore,
            Pageable pageable) {

        List<Criteria> filters = new ArrayList<>();

        if (name != null && !name.isBlank()) {
            // Busca "contém", sem diferenciar maiúsculas/minúsculas
            filters.add(Criteria.where("name").regex(Pattern.quote(name), "i"));
        }

        if (type != null) {
            filters.add(Criteria.where("type").is(type));
        }

        if (minPrice != null) {
            filters.add(Criteria.where("price").gte(minPrice));
        }

        if (maxPrice != null) {
            filters.add(Criteria.where("price").lte(maxPrice));
        }

        if (expDateBefore != null) {
            filters.add(Criteria.where("expDate").lte(expDateBefore));
        }

        Query query = new Query();
        if (!filters.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(filters.toArray(new Criteria[0])));
        }

        // A contagem precisa vir ANTES de aplicar paginação/ordenação,
        // senão o skip/limit distorce o total.
        long total = mongoTemplate.count(query, Product.class);

        query.with(pageable); // aplica skip, limit e sort do Pageable
        List<Product> products = mongoTemplate.find(query, Product.class);

        return new PageImpl<>(products, pageable, total);
    }
}