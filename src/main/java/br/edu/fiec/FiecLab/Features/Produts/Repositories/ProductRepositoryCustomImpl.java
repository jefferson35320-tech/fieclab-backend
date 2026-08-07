package br.edu.fiec.FiecLab.Features.Produts.Repositories;

import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.Product;
import br.edu.fiec.FiecLab.Features.Produts.ProdutsModel.Entities.ProductType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Product> findProductsByCriteria(
            String name,
            ProductType type,
            Double minPrice,
            Double maxPrice,
            LocalDate expDateBefore,
            Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // --- 1. CONSULTA PRINCIPAL (DADOS) ---
        CriteriaQuery<Product> query = cb.createQuery(Product.class);
        Root<Product> product = query.from(Product.class);

        List<Predicate> predicates = buildPredicates(cb, product, name, type, minPrice, maxPrice, expDateBefore);
        query.where(predicates.toArray(new Predicate[0]));

        // Aplica a ordenação vinda do Pageable
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                if (order.isAscending()) {
                    orders.add(cb.asc(product.get(order.getProperty())));
                } else {
                    orders.add(cb.desc(product.get(order.getProperty())));
                }
            });
            query.orderBy(orders);
        }

        TypedQuery<Product> typedQuery = entityManager.createQuery(query);

        // Aplica o deslocamento (offset) e o limite da página
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Product> products = typedQuery.getResultList();

        // --- 2. CONSULTA DE CONTAGEM (TOTAL ELEMENTS) ---
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Product> countRoot = countQuery.from(Product.class);

        List<Predicate> countPredicates = buildPredicates(cb, countRoot, name, type, minPrice, maxPrice, expDateBefore);
        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(products, pageable, total);
    }

    // Método auxiliar para reaproveitar os filtros entre a consulta de dados e a de contagem
    private List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            Root<Product> product,
            String name,
            ProductType type,
            Double minPrice,
            Double maxPrice,
            LocalDate expDateBefore) {

        List<Predicate> predicates = new ArrayList<>();

        if (name != null && !name.isBlank()) {
            predicates.add(cb.like(cb.lower(product.get("name")), "%" + name.toLowerCase() + "%"));
        }

        if (type != null) {
            predicates.add(cb.equal(product.get("type"), type));
        }

        if (minPrice != null) {
            predicates.add(cb.greaterThanOrEqualTo(product.get("price"), minPrice));
        }

        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(product.get("price"), maxPrice));
        }

        if (expDateBefore != null) {
            predicates.add(cb.lessThanOrEqualTo(product.get("expDate"), expDateBefore));
        }

        return predicates;
    }
}