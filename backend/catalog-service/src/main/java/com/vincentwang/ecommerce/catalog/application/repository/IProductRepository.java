package com.vincentwang.ecommerce.catalog.application.repository;

import com.vincentwang.ecommerce.catalog.domain.model.Product;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IProductRepository {

    int insert(Product product);

    Optional<Product> findById(long productId);

    List<Product> findPage(String userId, long offset, int limit);

    long count(String userId);

    boolean categoryExists(long categoryId);

    int update(Product product);

    int appendOutboxEvent(String eventId, String eventType, Product product, LocalDateTime occurredAt);
}
