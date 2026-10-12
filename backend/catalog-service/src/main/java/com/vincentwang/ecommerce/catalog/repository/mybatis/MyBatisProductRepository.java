package com.vincentwang.ecommerce.catalog.repository.mybatis;

import com.vincentwang.ecommerce.catalog.model.Product;
import com.vincentwang.ecommerce.catalog.repository.IProductRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisProductRepository implements IProductRepository {

    private final IProductMapper mapper;

    public MyBatisProductRepository(IProductMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public int insert(Product product) {
        return mapper.insert(product);
    }

    @Override
    public Optional<Product> findById(long productId) {
        return mapper.findById(productId);
    }

    @Override
    public List<Product> findPage(String userId, long offset, int limit) {
        return mapper.findPage(userId, offset, limit);
    }

    @Override
    public long count(String userId) {
        return mapper.count(userId);
    }

    @Override
    public boolean categoryExists(long categoryId) {
        return mapper.categoryExists(categoryId);
    }

    @Override
    public int update(Product product) {
        return mapper.update(product);
    }

    @Override
    public int appendOutboxEvent(String eventId, String eventType, Product product, LocalDateTime occurredAt) {
        return mapper.insertOutbox(eventId, eventType, product, occurredAt);
    }
}
