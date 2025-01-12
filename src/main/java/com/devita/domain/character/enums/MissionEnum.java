package com.devita.domain.character.enums;

import com.devita.common.constant.CategoryConstants;
import com.devita.domain.character.domain.RewardType;
import lombok.Getter;

@Getter
public enum MissionEnum {
    USER_TODO(new RewardType(RewardEnum.NUTRITION, 10), 10),         // 경험치 10, 일일 최대 10회
    DAILY_MISSION(new RewardType(RewardEnum.EXPERIENCE, 3), 1),   // 영양제 3개, 일일 최대 1회
    FREE_MISSION(new RewardType(RewardEnum.NUTRITION, 1), 3);     // 영양제 1개, 일일 최대 3회

    private final RewardType rewardType;
    private final int dailyLimit;    // 일일 제한

    MissionEnum(RewardType rewardType, int dailyLimit) {
        this.rewardType = rewardType;
        this.dailyLimit = dailyLimit;
    }

    public static MissionEnum fromCategory(String categoryName) {
        return switch (categoryName) {
            case CategoryConstants.DAILY_MISSION_CATEGORY -> DAILY_MISSION;
            case CategoryConstants.FREE_MISSION_CATEGORY -> FREE_MISSION;
            default -> USER_TODO;
        };
    }
}