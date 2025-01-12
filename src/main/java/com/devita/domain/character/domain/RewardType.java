package com.devita.domain.character.domain;

import com.devita.common.entity.BaseEntity;
import com.devita.domain.character.enums.RewardEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RewardType extends BaseEntity {
    private RewardEnum type;
    private int amount;
}