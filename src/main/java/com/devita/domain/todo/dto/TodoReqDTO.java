package com.devita.domain.todo.dto;

import java.time.LocalDate;

public record TodoReqDTO(
        Long categoryId,
        String title,
        LocalDate date
) {}