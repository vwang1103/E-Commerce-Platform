package com.vincentwang.ecommerce.catalog.repository;

import com.vincentwang.ecommerce.catalog.model.Category;
import java.util.List;

public interface ICategoryRepository {

    List<Category> findAll();
}
