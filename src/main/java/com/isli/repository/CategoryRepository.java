package com.isli.repository;

import com.isli.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findCategoryByName(String name);

    boolean existsByName(String name);
}
