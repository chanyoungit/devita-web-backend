package com.devita.domain.user.dto;

import com.devita.domain.user.domain.PreferredCategory;

import java.util.List;

public record PreferredCategoryRequest(
        List<PreferredCategory> categories
) {}
