package com.devita.domain.todo.dto;

import com.devita.domain.todo.domain.Todo;

import java.time.LocalDate;

public record CalenderDTO(
        Long todoId,
        Long categoryId,
        String title,
        Boolean status,
        LocalDate date
) {
    public static CalenderDTO fromEntity(Todo todo) {
        return new CalenderDTO(
                todo.getId(),
                todo.getCategory().getId(),
                todo.getTitle(),
                todo.getStatus(),
                todo.getDate()
        );
    }
}