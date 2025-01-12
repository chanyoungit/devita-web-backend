package com.devita.domain.category.dto;

import lombok.Builder;

@Builder
public record CategoryReqDTO(
        String name,
        String color
) {}
