package com.devita.domain.category.service;

import com.devita.common.exception.AccessDeniedException;
import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.ResourceNotFoundException;
import com.devita.common.exception.IllegalArgumentException;
import com.devita.domain.category.domain.Category;
import com.devita.domain.category.dto.CategoryReqDTO;
import com.devita.domain.category.dto.CategoryResDTO;
import com.devita.domain.category.repository.CategoryRepository;
import com.devita.domain.user.domain.User;
import com.devita.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    // 카테고리 추가
    public Category createCategory(Long userId, CategoryReqDTO categoryReqDto) {
        User user = userRepository.findById(userId).orElseThrow();

        boolean exists = categoryRepository.existsByUserIdAndName(userId, categoryReqDto.name());
        if (exists) {
            throw new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        Category category = new Category(user, categoryReqDto.name(), categoryReqDto.color());

        return categoryRepository.save(category);
    }

    public void deleteCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .filter(t -> t.getUser().getId().equals(userId))  // userId가 일치하는지 확인
                .orElseThrow(() -> new AccessDeniedException(ErrorCode.CATEGORY_ACCESS_DENIED));

        if (isMissionCategory(category.getName())) {
            throw new AccessDeniedException(ErrorCode.MISSION_CATEGORY_ACCESS_DENIED);
        }

        categoryRepository.delete(category);
    }

    private boolean isMissionCategory(String categoryName) {
        return List.of("일일 미션", "자율 미션").contains(categoryName);
    }

    public Category updateCategory(Long userId, Long categoryId, CategoryReqDTO categoryReqDto) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND));

        if (!category.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(ErrorCode.CATEGORY_ACCESS_DENIED);  // userId가 일치하지 않으면 AccessDeniedException 발생
        }

        validateCategoryRequest(categoryReqDto);

        category.setNameAndColor(categoryReqDto.name(), categoryReqDto.color());

        return categoryRepository.save(category);
    }

    private void validateCategoryRequest(CategoryReqDTO categoryReqDto) {
        if (categoryReqDto.name() == null || categoryReqDto.name().isBlank()) {
            throw new IllegalArgumentException(ErrorCode.INVALID_CATEGORY_NAME);
        }
        if (categoryReqDto.color() == null || categoryReqDto.color().isBlank()) {
            throw new IllegalArgumentException(ErrorCode.INVALID_CATEGORY_COLOR);
        }
    }

    public List<CategoryResDTO> findUserCategories(Long userId) {
        return categoryRepository.findByUserId(userId).stream()
                .map(category -> CategoryResDTO.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .color(category.getColor())
                        .build())
                .toList();
    }
}
