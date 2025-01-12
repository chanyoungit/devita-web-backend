package com.devita.domain.user.dto;

import com.devita.domain.user.domain.PreferredCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

public record PreferredCategoryResponse(
        List<PreferredCategory> categories
) {}