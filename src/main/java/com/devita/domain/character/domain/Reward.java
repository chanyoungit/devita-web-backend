package com.devita.domain.character.domain;

import com.devita.common.entity.BaseEntity;
import com.devita.common.exception.ErrorCode;
import com.devita.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Reward extends BaseEntity {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId  // user_id를 PK로 사용
    @JoinColumn(name = "user_id")
    private User user;

    private int experience;
    private int nutrition;

    @Builder
    public Reward(User user, int experience, int nutrition) {
        this.user = user;
        this.experience = experience;
        this.nutrition = nutrition;
    }

    public void addExperience(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException(ErrorCode.INVALID_REWARD_VALUE.getMessage());
        }
        this.experience += amount;
    }

    public void addNutrition(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException(ErrorCode.INVALID_REWARD_VALUE.getMessage());
        }
        this.nutrition += amount;
    }

    public void useNutrition(){
        this.experience += 30;
        this.nutrition -= 1;
    }
}