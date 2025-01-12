package com.devita.domain.mission.service;

import com.devita.common.exception.AiServerConnectionException;
import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.ResourceNotFoundException;
import com.devita.domain.category.domain.Category;
import com.devita.domain.todo.domain.*;
import com.devita.domain.category.repository.CategoryRepository;
import com.devita.domain.mission.dto.ai.*;
import com.devita.domain.mission.dto.client.FreeSaveReqDTO;
import com.devita.domain.todo.repository.TodoRepository;
import com.devita.domain.user.domain.AuthProvider;
import com.devita.domain.user.domain.User;
import com.devita.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private TodoRepository todoRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MissionService missionService;

    @Value("${ai.address}")
    private String aiAddress;

    private User testUser;
    private Category testCategory;
    private static final Long USER_ID = 1L;
    private static final String USER_EMAIL = "test@test.com";
    private static final String USER_NICKNAME = "testUser";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(USER_EMAIL)
                .nickname(USER_NICKNAME)
                .provider(AuthProvider.KAKAO)
                .build();
        testUser.setId(USER_ID);

        testCategory = new Category(testUser, "자율 미션", "#000000");
    }

    @Test
    @DisplayName("일일 미션 요청 성공")
    void requestDailyMission_Success() {
        // given
        List<String> categories = List.of("Java", "Spring");
        DailyMissionAiResDTO expectedResponse = new DailyMissionAiResDTO("Daily Mission Test");

        when(restTemplate.postForObject(
                eq(aiAddress + "/ai/v1/mission/daily"),
                any(DailyMissionAiReqDTO.class),
                eq(DailyMissionAiResDTO.class)
        )).thenReturn(expectedResponse);

        // when
        DailyMissionAiResDTO result = missionService.requestDailyMission(USER_ID, categories);

        // then
        assertEquals(expectedResponse.missionTitle(), result.missionTitle());
    }

    @Test
    @DisplayName("자유 미션 요청 성공")
    void requestFreeMission_Success() {
        // given
        MissionAiResDTO missionAiResDTO1 = new MissionAiResDTO(1, "Spring1");

        MissionAiResDTO missionAiResDTO2 = new MissionAiResDTO(2, "Spring2");

        List<MissionAiResDTO> missions = List.of(missionAiResDTO1, missionAiResDTO2);
        FreeMissionAiResDTO expectedResponse = new FreeMissionAiResDTO(missions);

        when(restTemplate.postForObject(
                eq(aiAddress + "/ai/v1/mission/free"),
                any(FreeMissionAiReqDTO.class),
                eq(FreeMissionAiResDTO.class)
        )).thenReturn(expectedResponse);

        // when
        List<MissionAiResDTO> result = missionService.requestFreeMission(USER_ID, "Spring");

        // then
        assertEquals(missions.size(), result.size());
        assertEquals(missions.get(0).missionTitle(), result.get(0).missionTitle());
        assertEquals(missions.get(1).missionTitle(), result.get(1).missionTitle());
    }

    @Test
    @DisplayName("자유 미션 저장 성공")
    void saveFreeMission_Success() {
        // given
        FreeSaveReqDTO reqDTO = new FreeSaveReqDTO("Free Mission Test");

        Todo expectedTodo = Todo.builder()
                .user(testUser)
                .category(testCategory)
                .title(reqDTO.missionTitle())
                .status(false)
                .date(LocalDate.now(ZoneId.of("Asia/Seoul")))
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByUserIdAndName(USER_ID, "자율 미션"))
                .thenReturn(Optional.of(testCategory));
        when(todoRepository.save(any(Todo.class))).thenReturn(expectedTodo);

        // when
        Todo result = missionService.saveFreeMission(USER_ID, reqDTO);

        // then
        assertNotNull(result);
        assertEquals(reqDTO.missionTitle(), result.getTitle());
        assertEquals(testUser, result.getUser());
        assertEquals(testCategory, result.getCategory());
    }

    @Test
    @DisplayName("AI 서버 오류 발생 시 일일 미션 요청 실패")
    void requestDailyMission_AiServerError() {
        // given
        when(restTemplate.postForObject(
                anyString(),
                any(DailyMissionAiReqDTO.class),
                eq(DailyMissionAiResDTO.class)
        )).thenThrow(new RestClientException("AI Server Error"));

        // when
        AiServerConnectionException exception = assertThrows(AiServerConnectionException.class, () -> missionService.requestDailyMission(USER_ID, List.of("Java")));

        // then
        assertEquals(ErrorCode.AI_SERVER_ERROR, exception.getErrorCode());
    }

    @Test
    @DisplayName("사용자 미발견 시 자유 미션 저장 실패")
    void saveFreeMission_UserNotFound() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // when
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> missionService.saveFreeMission(USER_ID, new FreeSaveReqDTO("")));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("카테고리 미발견 시 자유 미션 저장 실패")
    void saveFreeMission_CategoryNotFound() {
        // given
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findByUserIdAndName(USER_ID, "자율 미션"))
                .thenReturn(Optional.empty());

        // when
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> missionService.saveFreeMission(USER_ID, new FreeSaveReqDTO("")));

        // then
        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.getErrorCode());
    }
}
