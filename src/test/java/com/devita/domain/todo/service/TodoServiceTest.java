package com.devita.domain.todo.service;

import com.devita.common.exception.AccessDeniedException;
import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.ResourceNotFoundException;
import com.devita.domain.category.domain.Category;
import com.devita.domain.todo.domain.*;
import com.devita.domain.category.repository.CategoryRepository;
import com.devita.domain.character.service.RewardService;
import com.devita.domain.todo.dto.CalenderDTO;
import com.devita.domain.todo.dto.TodoReqDTO;
import com.devita.domain.todo.repository.TodoRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RewardService rewardService;

    @InjectMocks
    private TodoService todoService;

    private User testUser;
    private Category testCategory;
    private Todo testTodo;
    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@test.com";
    private static final String USER_NICKNAME = "testUser";
    private static final Long TODO_ID = 1L;
    private static final Long CATEGORY_ID = 1L;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(USER_EMAIL)
                .nickname(USER_NICKNAME)
                .provider(AuthProvider.KAKAO)
                .profileImage("profile.jpg")
                .build();
        testUser.setId(USER_ID);

        testCategory = new Category(testUser, "Study", "#000000");
        testCategory.setId(CATEGORY_ID);

        testTodo = Todo.builder()
                .user(testUser)
                .category(testCategory)
                .title("Test Todo")
                .status(false)
                .date(LocalDate.now())
                .build();
        testTodo.setId(TODO_ID);
    }

    @Test
    @DisplayName("할 일 추가 성공")
    void addTodo_Success() {
        // given
        TodoReqDTO reqDTO = new TodoReqDTO(CATEGORY_ID, "New Todo", LocalDate.now());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(testCategory));
        when(todoRepository.save(any(Todo.class))).thenAnswer(invocations -> {
            Todo savedTodo = invocations.getArgument(0);
            savedTodo.setId(TODO_ID);
            return savedTodo;
        });

        // when
        Todo result = todoService.addTodo(USER_ID, reqDTO);

        // then
        assertNotNull(result);
        assertEquals(result.getTitle(), reqDTO.title());
        assertEquals(result.getDate(), reqDTO.date());
    }

    @Test
    @DisplayName("할 일 삭제 성공")
    void deleteTodo_Success() {
        // given
        when(todoRepository.findById(TODO_ID)).thenReturn(Optional.of(testTodo));

        // when
        todoService.deleteTodo(USER_ID, TODO_ID);

        // then
        verify(todoRepository).delete(testTodo);
    }

    @Test
    @DisplayName("할 일 수정 성공")
    void updateTodo_Success() {
        // given
        TodoReqDTO reqDTO = new TodoReqDTO(CATEGORY_ID, "Updated Todo", LocalDate.now());

        when(todoRepository.findById(TODO_ID)).thenReturn(Optional.of(testTodo));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(testCategory));
        when(todoRepository.save(any(Todo.class))).thenReturn(testTodo);

        // when
        Todo result = todoService.updateTodo(USER_ID, TODO_ID, reqDTO);

        // then
        assertNotNull(result);
        assertEquals(reqDTO.title(), result.getTitle());
        verify(todoRepository).save(any(Todo.class));
    }

    @Test
    @DisplayName("할 일 상태 전환 성공")
    void toggleTodo_Success() {
        // given
        when(todoRepository.findById(TODO_ID)).thenReturn(Optional.of(testTodo));
        when(todoRepository.save(any(Todo.class))).thenReturn(testTodo);

        // when
        todoService.toggleTodo(USER_ID, TODO_ID);

        // then
        assertTrue(testTodo.getStatus());
        verify(todoRepository).save(testTodo);
        verify(rewardService).processReward(testUser, testTodo);
    }

    @Test
    @DisplayName("주간 캘린더 조회 성공")
    void getCalendar_Weekly_Success() {
        // given
        LocalDate today = LocalDate.now();
        List<Todo> todos = List.of(testTodo);

        when(todoRepository.findByUserIdAndDateBetween(
                eq(USER_ID),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(todos);

        // when
        List<CalenderDTO> result = todoService.getCalendar(USER_ID, "weekly");

        // then
        assertFalse(result.isEmpty());
        verify(todoRepository).findByUserIdAndDateBetween(
                eq(USER_ID),
                any(LocalDate.class),
                any(LocalDate.class)
        );
    }

    @Test
    @DisplayName("사용자 없음으로 인해 할 일 추가 실패")
    void addTodo_UserNotFound() {
        // given
        TodoReqDTO reqDTO = new TodoReqDTO(null, "", null);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> todoService.addTodo(USER_ID, reqDTO)
        );

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("다른 사용자의 할 일 수정 접근 거부")
    void updateTodo_AccessDenied() {
        // given
        User otherUser = User.builder()
                .email("other@test.com")
                .nickname("otherUser")
                .provider(AuthProvider.KAKAO)
                .profileImage("profile.jpg")
                .build();
        otherUser.setId(2L);

        Todo otherTodo = Todo.builder()
                .user(otherUser)
                .category(testCategory)
                .title("Other Todo")
                .status(false)
                .date(LocalDate.now())
                .build();

        TodoReqDTO reqDTO = new TodoReqDTO(null, "", null);
        when(todoRepository.findById(TODO_ID)).thenReturn(Optional.of(otherTodo));

        // when
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> todoService.updateTodo(USER_ID, TODO_ID, reqDTO)
        );

        // then
        assertEquals(ErrorCode.TODO_ACCESS_DENIED, exception.getErrorCode());
    }
}
