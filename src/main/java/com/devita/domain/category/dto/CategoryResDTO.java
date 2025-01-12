package com.devita.domain.category.dto;

import lombok.Builder;

@Builder
public record CategoryResDTO(
        Long id,
        String name,
        String color
) {}
