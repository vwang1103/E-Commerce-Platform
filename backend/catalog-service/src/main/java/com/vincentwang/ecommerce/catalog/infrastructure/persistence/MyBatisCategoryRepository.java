package com.vincentwang.ecommerce.catalog.infrastructure.persistence;

import com.vincentwang.ecommerce.catalog.application.repository.ICategoryRepository;
import com.vincentwang.ecommerce.catalog.domain.model.Category;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyBatisCategoryRepository implements ICategoryRepository {

    private final IProductMapper mapper;

    public MyBatisCategoryRepository(IProductMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Category> findAll() {
        return mapper.findCategories();
    }
}
