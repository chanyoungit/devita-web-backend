package com.devita.domain.category.repository;

import com.devita.domain.category.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserId(Long userId);
    Optional<Category> findByUserIdAndName(Long userId, String name);
    boolean existsByUserIdAndName(Long userId, String name);
}
