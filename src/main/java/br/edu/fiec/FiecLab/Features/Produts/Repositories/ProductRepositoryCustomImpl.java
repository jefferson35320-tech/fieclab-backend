package br.edu.fiec.FiecLab.Features.Produts.Repositories;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Categoria;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public ProductRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    @Override
    public Product addRating(String id, double estrelas) {
        Query query = new Query(Criteria.where("id").is(id));

        Update update = new Update()
                .inc("valorAvaliacaoTotal", estrelas)
                .inc("avaliacaoTotais", 1);

        return mongoTemplate.findAndModify(
                query, update, FindAndModifyOptions.options().returnNew(true), Product.class);
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

    @Override
    public Product decrementStock(String id, int quantidade) {
        // só encontra o produto se, após a baixa, o estoque continuar >= quantidade mínima
        Criteria disponivel = Criteria.expr(
                ComparisonOperators.valueOf(
                                ArithmeticOperators.Subtract
                                        .valueOf("estoque.quantidade")
                                        .subtract("estoque.quantidade_minima"))
                        .greaterThanEqualToValue(quantidade));

        Query query = new Query(Criteria.where("id").is(id).andOperator(disponivel));

        Update update = new Update()
                .inc("estoque.quantidade", -quantidade)
                .set("estoque.dataAtualizacao", LocalDateTime.now());

        // retorna null se o produto não existe ou se a baixa passaria do mínimo
        return mongoTemplate.findAndModify(
                query, update, FindAndModifyOptions.options().returnNew(true), Product.class);
    }
}