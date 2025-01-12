package com.devita.domain.mission.controller;


import com.devita.common.response.ApiResponse;
import com.devita.domain.mission.dto.ai.MissionAiResDTO;
import com.devita.domain.mission.dto.client.DailyMissionResDTO;
import com.devita.domain.mission.dto.client.FreeMissionReqDTO;
import com.devita.domain.mission.dto.client.FreeSaveReqDTO;
import com.devita.domain.mission.service.MissionService;
import com.devita.domain.todo.domain.Todo;
import com.devita.domain.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mission")
@Slf4j
public class MissionController {
    private final MissionService missionService;
    private final TodoRepository todoRepository;

    @GetMapping("/daily")
    public ApiResponse<DailyMissionResDTO> getDailyMission(@AuthenticationPrincipal Long userId){
        Todo dailyMission = todoRepository.findTodosByUserIdAndCategoryNameAndDate(userId, "일일 미션", LocalDate.now());

        DailyMissionResDTO dailyMissionResDTO = new DailyMissionResDTO(dailyMission.getId(), dailyMission.getTitle());

        log.info(dailyMissionResDTO.toString());
        return ApiResponse.success(dailyMissionResDTO);

    }

    @PostMapping("/free")
    public ApiResponse<List<MissionAiResDTO>> getFreeMission(@AuthenticationPrincipal Long userId, @RequestBody FreeMissionReqDTO freeMissionReqDTO){
        List<MissionAiResDTO> freeMissions = missionService.requestFreeMission(userId, freeMissionReqDTO.subCategory());

        return ApiResponse.success(freeMissions);
    }

    @PostMapping("/free/save")
    public ApiResponse<Long> saveFreeMission(@AuthenticationPrincipal Long userId, @RequestBody FreeSaveReqDTO freeSaveReqDTO){
        Long todoId = missionService.saveFreeMission(userId, freeSaveReqDTO).getId();

        return ApiResponse.success(todoId);
    }
}
