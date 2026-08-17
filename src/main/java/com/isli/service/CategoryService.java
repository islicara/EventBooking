package com.isli.service;

import com.isli.dto.CategoryDTO;
import com.isli.entity.Category;
import com.isli.exception.DuplicateResourceException;
import com.isli.exception.ResourceNotFoundException;
import com.isli.mapper.EntityMapper;
import com.isli.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private static final Logger log = LogManager.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryDTO create(CategoryDTO request) {
        log.trace("Entering create() name='{}'", request.getName());
        if (categoryRepository.existsByName(request.getName())) {
            log.warn("Rejected category create: '{}' already exists", request.getName());
            throw new DuplicateResourceException("Category already exists: " + request.getName());
        }
        Category category = categoryRepository.save(Category.builder().name(request.getName()).build());
        log.info("Created category '{}'", category.getName());
        return EntityMapper.toResponse(category);
    }

    @Transactional
    public CategoryDTO update(Long id, CategoryDTO request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        log.debug("Renaming category {} from '{}' to '{}'", id, category.getName(), request.getName());
        category.setName(request.getName());
        log.info("Updated category {}", id);
        return EntityMapper.toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        categoryRepository.delete(category);
        log.info("Deleted category {}", id);
    }

    @Transactional
    public List<CategoryDTO> getAll() {
        log.trace("Fetching all categories");
        return categoryRepository.findAll().stream().map(EntityMapper::toResponse).toList();
    }
}