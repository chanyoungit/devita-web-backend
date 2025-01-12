package com.devita.domain.mission.service;

import com.devita.common.exception.AiServerConnectionException;
import com.devita.common.exception.ErrorCode;
import com.devita.common.exception.ResourceNotFoundException;
import com.devita.domain.category.domain.Category;
import com.devita.domain.category.repository.CategoryRepository;
import com.devita.domain.mission.dto.ai.*;
import com.devita.domain.mission.dto.client.FreeSaveReqDTO;
import com.devita.domain.todo.domain.Todo;
import com.devita.domain.todo.repository.TodoRepository;
import com.devita.domain.user.domain.User;
import com.devita.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MissionService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final RestTemplate restTemplate;
    private final TodoRepository todoRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Value("${ai.address}")
    private String aiAddress;

    private static final String DAILY_MISSION_API = "/ai/v1/mission/daily";
    private static final String FREE_MISSION_API = "/ai/v1/mission/free";
    private static final String FREE_MISSION = "자율 미션";

    public DailyMissionAiResDTO requestDailyMission(Long userId, List<String> categories) {
        try {
            DailyMissionAiReqDTO request = new DailyMissionAiReqDTO(userId, categories);
            log.debug("Requesting daily mission for user: {}, categories: {}", userId, categories);

            DailyMissionAiResDTO response = restTemplate.postForObject(
                    aiAddress + DAILY_MISSION_API,
                    request,
                    DailyMissionAiResDTO.class
            );

            return Objects.requireNonNull(response, "AI server returned null response");
        } catch (RestClientException e) {
            log.error("Failed to request daily mission from AI server", e);
            throw new AiServerConnectionException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    public List<MissionAiResDTO> requestFreeMission(Long userId, String subCategory) {
        try {
            FreeMissionAiReqDTO request = new FreeMissionAiReqDTO(userId, subCategory);
            log.debug("Requesting free mission for user: {}, subCategory: {}", userId, subCategory);

            FreeMissionAiResDTO response = restTemplate.postForObject(
                    aiAddress + FREE_MISSION_API,
                    request,
                    FreeMissionAiResDTO.class
            );

            return response != null ? response.missions() : Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Failed to request free mission from AI server", e);
            throw new AiServerConnectionException(ErrorCode.AI_SERVER_ERROR);
        }
    }

    @Transactional
    public Todo saveFreeMission(Long userId, FreeSaveReqDTO freeSaveReqDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));

        Category category = categoryRepository.findByUserIdAndName(userId, FREE_MISSION)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CATEGORY_NOT_FOUND));

        Todo todo = Todo.builder()
                .user(user)
                .category(category)
                .title(freeSaveReqDTO.missionTitle())
                .status(false)
                .date(LocalDate.now(KOREA_ZONE))
                .build();

        return todoRepository.save(todo);
    }
}
