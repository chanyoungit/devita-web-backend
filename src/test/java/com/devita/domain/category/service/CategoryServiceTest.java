package com.devita.domain.category.service;

import com.devita.common.exception.AccessDeniedException;
import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.ResourceNotFoundException;
import com.devita.common.exception.IllegalArgumentException;
import com.devita.domain.category.domain.Category;
import com.devita.domain.category.dto.CategoryReqDTO;
import com.devita.domain.category.dto.CategoryResDTO;
import com.devita.domain.category.repository.CategoryRepository;
import com.devita.domain.user.domain.AuthProvider;
import com.devita.domain.user.domain.User;
import com.devita.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User testUser;
    private Category testCategory;
    private CategoryReqDTO categoryReqDTO;

    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@test.com";
    private static final String USER_NICKNAME = "testUser";
    private static final Long CATEGORY_ID = 1L;
    private static final String CATEGORY_NAME = "test category";
    private static final String CATEGORY_COLOR = "#000000";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(USER_EMAIL)
                .nickname(USER_NICKNAME)
                .provider(AuthProvider.KAKAO)
                .build();
        testUser.setId(USER_ID);

        testCategory = new Category(testUser, CATEGORY_NAME, CATEGORY_COLOR);

        categoryReqDTO = CategoryReqDTO.builder()
                .name(CATEGORY_NAME)
                .color(CATEGORY_COLOR)
                .build();
    }

    @Test
    @DisplayName("카테고리 생성 성공")
    void createCategory_Success() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(categoryRepository.existsByUserIdAndName(USER_ID, CATEGORY_NAME)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // when
        Category result = categoryService.createCategory(USER_ID, categoryReqDTO);

        // then
        assertNotNull(result);
        assertEquals(CATEGORY_NAME, result.getName());
        assertEquals(CATEGORY_COLOR, result.getColor());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("카테고리 삭제 성공")
    void deleteCategory_Success() {
        // given
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(testCategory));

        // when
        categoryService.deleteCategory(USER_ID, CATEGORY_ID);

        // then
        verify(categoryRepository).delete(testCategory);
    }

    @Test
    @DisplayName("카테고리 업데이트 성공")
    void updateCategory_Success() {
        // given
        CategoryReqDTO updateReq = CategoryReqDTO.builder()
                .name("Updated Category")
                .color("#FFFFFF")
                .build();

        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(Category.class)))
                .thenReturn(testCategory);

        // when
        Category result = categoryService.updateCategory(USER_ID, CATEGORY_ID, updateReq);

        // then
        assertNotNull(result);
        assertEquals("Updated Category", result.getName());
        assertEquals("#FFFFFF", result.getColor());
    }

    @Test
    @DisplayName("사용자 카테고리 조회 성공")
    void findUserCategories_Success() {
        // given
        List<Category> categories = Arrays.asList(
                new Category(testUser, "Category 1", "#000000"),
                new Category(testUser, "Category 2", "#FFFFFF")
        );
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(categories);

        // when
        List<CategoryResDTO> results = categoryService.findUserCategories(USER_ID);

        // then
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Category 1", results.get(0).name());
        assertEquals("Category 2", results.get(1).name());
    }

    @Test
    @DisplayName("중복 카테고리 생성 시 오류")
    void createCategory_DuplicateCategory() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(categoryRepository.existsByUserIdAndName(USER_ID, CATEGORY_NAME)).thenReturn(true);

        // when
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> categoryService.createCategory(USER_ID, categoryReqDTO));

        // then
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("미션 카테고리 삭제 시 접근 거부")
    void deleteCategory_MissionCategoryFail() {
        // given
        Category missionCategory = new Category(testUser, "일일 미션", "#000000");
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(missionCategory));

        // when
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> categoryService.deleteCategory(USER_ID, CATEGORY_ID));

        // then
        assertEquals(ErrorCode.MISSION_CATEGORY_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("잘못된 사용자로 카테고리 업데이트 시 오류")
    void updateCategory_WrongUser() {
        // given
        Long wrongUserId = 2L;
        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(testCategory));

        // when
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> categoryService.updateCategory(wrongUserId, CATEGORY_ID, categoryReqDTO));

        // then
        assertEquals(ErrorCode.CATEGORY_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("잘못된 입력으로 카테고리 업데이트 시 오류")
    void updateCategory_InvalidInput() {
        // given
        CategoryReqDTO invalidReq = CategoryReqDTO.builder()
                .name("")
                .color("#FFFFFF")
                .build();

        when(categoryRepository.findById(CATEGORY_ID))
                .thenReturn(Optional.of(testCategory));

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> categoryService.updateCategory(USER_ID, CATEGORY_ID, invalidReq));

        // then
        assertEquals(ErrorCode.INVALID_CATEGORY_NAME, exception.getErrorCode());
    }
}