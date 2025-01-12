package com.devita.common.oauth;

import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.ResourceNotFoundException;
import com.devita.domain.category.domain.Category;
import com.devita.domain.category.dto.CategoryReqDTO;
import com.devita.domain.category.repository.CategoryRepository;
import com.devita.domain.category.service.CategoryService;
import com.devita.domain.character.domain.Reward;
import com.devita.domain.character.repository.RewardRepository;
import com.devita.domain.mission.dto.ai.DailyMissionAiResDTO;
import com.devita.domain.todo.domain.Todo;
import com.devita.domain.todo.repository.TodoRepository;
import com.devita.domain.user.domain.AuthProvider;
import com.devita.domain.user.domain.User;
import com.devita.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final RewardRepository rewardRepository;

    private final CategoryRepository categoryRepository;
    private final TodoRepository todoRepository;

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!"kakao".equals(registrationId)) {
            throw new OAuth2AuthenticationException(ErrorCode.INVALID_TOKEN.getMessage());
        }

        Map<String, Object> attributes = oAuth2User.getAttributes();
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");

        String email = (String) kakaoAccount.get("email");
        String nickname = (String) properties.get("nickname");
        String profileImage = (String) properties.get("profile_image");

        if (email == null) {
            throw new OAuth2AuthenticationException(ErrorCode.TOKEN_NOT_FOUND.getMessage());
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .nickname(nickname)
                            .provider(AuthProvider.KAKAO)
                            .profileImage(profileImage)
                            .build();

                    User savedUser = userRepository.save(newUser);

                    Reward reward = Reward.builder()
                            .user(savedUser)
                            .experience(0)
                            .nutrition(0)
                            .build();
                    rewardRepository.save(reward);

                    createDefaultCategories(savedUser.getId());
                    createDefaultInfo(savedUser);

                    return savedUser;
                });

        user.updateNickname(nickname);
        userRepository.save(user);

        log.info("유저 로그인 성공!");
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("USER")),
                attributes,
                "id"
        );
    }

    private void createDefaultCategories(Long userId) {
        String[] defaultCategories = {"일반", "일일 미션", "자율 미션"};
        String[] defaultColors = {"#6DC2FF", "#086BFF", "#7DB1FF"};

        for (int i = 0; i < defaultCategories.length; i++) {
            CategoryReqDTO categoryReqDto = CategoryReqDTO.builder()
                    .name(defaultCategories[i])
                    .color(defaultColors[i])
                    .build();

            categoryService.createCategory(userId, categoryReqDto);
        }
    }

    private void createDefaultInfo(User user){
        /*
        사용자 생성 시 일일 미션 미리 넣어놓기
        카테고리 몇개 넣어 놓기
         */
        categoryService.createCategory(user.getId(), new CategoryReqDTO("식습관", "#000000"));
        categoryService.createCategory(user.getId(), new CategoryReqDTO("운동", "#000000"));


        try {
            // 해당 사용자의 '일일 미션' 카테고리 찾기
            Category dailyMissionCategory = categoryRepository.findByUserIdAndName(user.getId(), "일일 미션")
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND));

            log.info(dailyMissionCategory.toString());

            // AI 서버에 Daily Mission 요청
            DailyMissionAiResDTO missionResponse = new DailyMissionAiResDTO("다형성 공부하기");

            // 미션 생성
            Todo mission = Todo.builder()
                    .user(user)
                    .category(dailyMissionCategory)
                    .title(missionResponse.missionTitle())
                    .status(false)
                    .date(LocalDate.now(KOREA_ZONE))
                    .build();


            todoRepository.save(mission);
            log.info("사용자 {}의 미션 생성 완료: {}", user.getId(), missionResponse.missionTitle());

        } catch (ResourceNotFoundException e) {
            log.error("사용자 {}의 강제 미션 카테고리를 찾을 수 없습니다.", user.getId());
        } catch (Exception e) {
            log.error("사용자 {}의 미션 생성 중 오류 발생: {}", user.getId(), e.getMessage());
        }
    }
}