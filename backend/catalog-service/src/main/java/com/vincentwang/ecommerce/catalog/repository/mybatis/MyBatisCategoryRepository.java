package com.vincentwang.ecommerce.catalog.repository.mybatis;

import com.vincentwang.ecommerce.catalog.model.Category;
import com.vincentwang.ecommerce.catalog.repository.ICategoryRepository;
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
