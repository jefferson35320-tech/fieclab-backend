package br.edu.fiec.FiecLab.Features.Produts.Repositories;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Categoria;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.math.BigDecimal;
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
            Categoria categoria,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            LocalDate expDateBefore,
            Pageable pageable) {

        List<Criteria> filters = new ArrayList<>();

        if (name != null && !name.isBlank()) {
            filters.add(Criteria.where("name")
                    .regex(Pattern.compile(Pattern.quote(name.trim()), Pattern.CASE_INSENSITIVE)));
        }

        if (categoria != null) {
            filters.add(Criteria.where("categoria.nome").is(categoria));
        }

        if (minPrice != null && maxPrice != null) {
            filters.add(Criteria.where("price").gte(minPrice).lte(maxPrice));
        } else if (minPrice != null) {
            filters.add(Criteria.where("price").gte(minPrice));
        } else if (maxPrice != null) {
            filters.add(Criteria.where("price").lte(maxPrice));
        }

        if (expDateBefore != null) {
            filters.add(Criteria.where("validade").lte(expDateBefore));
        }

        Query query = new Query();
        if (!filters.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(filters.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Product.class);
        List<Product> content = mongoTemplate.find(query.with(pageable), Product.class);

        return PageableExecutionUtils.getPage(content, pageable, () -> total);
    }
}