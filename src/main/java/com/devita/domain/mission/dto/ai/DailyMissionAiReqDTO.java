package com.devita.domain.mission.dto.ai;

import java.util.List;

public record DailyMissionAiReqDTO(
        Long userId,
        List<String> categories
) {}
