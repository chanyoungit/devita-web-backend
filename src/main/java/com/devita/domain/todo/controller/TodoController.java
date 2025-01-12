package com.devita.domain.todo.controller;

import com.devita.common.response.ApiResponse;
import com.devita.domain.todo.dto.CalenderDTO;
import com.devita.domain.todo.dto.TodoReqDTO;
import com.devita.domain.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/todo")
@RequiredArgsConstructor
@Slf4j
public class TodoController {

    private final TodoService todoService;

    @GetMapping("/calendar")
    public ApiResponse<List<CalenderDTO>> getCalendar(@AuthenticationPrincipal Long userId, @RequestParam String viewType) {
        log.info("유저 정보를 성공적으로 받아와서 캘린더를 호출합니다.");
        List<CalenderDTO> todos = todoService.getCalendar(userId, viewType);
        return ApiResponse.success(todos);
    }

    @PostMapping
    public ApiResponse<Long> addTodo(@AuthenticationPrincipal Long userId, @RequestBody TodoReqDTO todoReqDTO) {
        Long todoId = todoService.addTodo(userId, todoReqDTO).getId();

        return ApiResponse.success(todoId);
    }

    @PutMapping("/{todoId}")
    public ApiResponse<Long> updateTodo(@AuthenticationPrincipal Long userId, @PathVariable Long todoId, @RequestBody TodoReqDTO todoReqDTO) {
        todoService.updateTodo(userId, todoId, todoReqDTO);

        return ApiResponse.success(todoId);
    }

    @DeleteMapping("/{todoId}")
    public ApiResponse<Void> deleteTodo(@AuthenticationPrincipal Long userId, @PathVariable Long todoId) {
        todoService.deleteTodo(userId, todoId);

        return ApiResponse.success(null);
    }

    @PutMapping("/{todoId}/toggle")
    public ApiResponse<Void> toggleTodoCompletion(@AuthenticationPrincipal Long userId, @PathVariable Long todoId) {
        todoService.toggleTodo(userId, todoId);

        return ApiResponse.success(null);
    }


}