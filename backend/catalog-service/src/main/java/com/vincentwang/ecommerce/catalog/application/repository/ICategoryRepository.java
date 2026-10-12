package com.vincentwang.ecommerce.catalog.application.repository;

import com.vincentwang.ecommerce.catalog.domain.model.Category;
import java.util.List;

public interface ICategoryRepository {

    List<Category> findAll();
}
