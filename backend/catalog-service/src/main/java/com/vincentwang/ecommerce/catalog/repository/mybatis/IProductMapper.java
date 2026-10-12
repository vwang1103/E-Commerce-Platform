package com.vincentwang.ecommerce.catalog.repository.mybatis;

import com.vincentwang.ecommerce.catalog.messaging.CatalogOutboxEvent;
import com.vincentwang.ecommerce.catalog.model.Category;
import com.vincentwang.ecommerce.catalog.model.Product;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IProductMapper {

    int insert(Product product);

    Optional<Product> findById(long productId);

    List<Product> findPage(@Param("userId") String userId, @Param("offset") long offset, @Param("limit") int limit);

    long count(@Param("userId") String userId);

    List<Product> searchLike(@Param("query") String query, @Param("offset") long offset, @Param("limit") int limit);

    long countLike(@Param("query") String query);

    boolean categoryExists(long categoryId);

    List<Category> findCategories();

    int update(Product product);

    int insertOutbox(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("product") Product product,
            @Param("occurredAt") LocalDateTime occurredAt);

    List<CatalogOutboxEvent> findUnpublishedOutbox(@Param("limit") int limit);

    int markOutboxPublished(@Param("eventId") String eventId);
}
