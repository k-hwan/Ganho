package com.ssafy.ganhoho.domain.schedule.controller;

import com.ssafy.ganhoho.domain.schedule.dto.PersonalScheduleRequestDto;
import com.ssafy.ganhoho.domain.schedule.dto.PersonalScheduleResponseDto;
import com.ssafy.ganhoho.domain.schedule.dto.ScheduleDetailResponseDto;
import com.ssafy.ganhoho.domain.schedule.service.PersonalScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.ssafy.ganhoho.global.auth.dto.CustomUserDetails;
import com.ssafy.ganhoho.global.constant.ErrorCode;
import com.ssafy.ganhoho.global.error.CustomException;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
 

@Slf4j
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class PersonalScheduleController implements com.ssafy.ganhoho.global.docs.schedule.PersonalScheduleDocs {

    private final PersonalScheduleService personalScheduleService;

    private CustomUserDetails validateToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
        
        if (!(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new CustomException(ErrorCode.EXPIRED_ACCESS_TOKEN);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (userDetails.getUserId() == null) {
            throw new CustomException(ErrorCode.MISSING_REQUIRED_USER_DATA);
        }

        return userDetails;
    }

    @PostMapping("/personal")
    public ResponseEntity<PersonalScheduleResponseDto> addPersonalSchedule(
            @Parameter(description = "추가할 일정 정보", required = true) @RequestBody PersonalScheduleRequestDto requestDto) {
        CustomUserDetails userDetails = validateToken();
        
        if (requestDto == null) {
            throw new CustomException(ErrorCode.MISSING_REQUIRED_FIELDS);
        }

        Long memberId = userDetails.getUserId();
        PersonalScheduleResponseDto createdSchedule = personalScheduleService.addPersonalSchedule(requestDto, memberId);
        return new ResponseEntity<>(createdSchedule, HttpStatus.CREATED);
    }

    @GetMapping("/personal")
    public ResponseEntity<List<ScheduleDetailResponseDto>> getPersonalSchedules() {
        CustomUserDetails userDetails = validateToken();
        Long memberId = userDetails.getUserId();
        List<ScheduleDetailResponseDto> schedules = personalScheduleService.getFormattedPersonalSchedules(memberId);
        if (schedules == null || schedules.isEmpty()) {
            throw new CustomException(ErrorCode.NOT_EXIST_DATA);
        }
        
        return new ResponseEntity<>(schedules, HttpStatus.OK);
    }

    @PutMapping("/personal/{scheduleId}")
    public ResponseEntity<Map<String, Boolean>> updatePersonalSchedule(
            @Parameter(description = "수정할 일정의 ID") @PathVariable Long scheduleId,
            @Parameter(description = "수정할 일정 정보", required = true) @RequestBody PersonalScheduleRequestDto requestDto) {
        CustomUserDetails userDetails = validateToken();

        if (scheduleId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_PARAMETERS);
        }

        if (requestDto == null) {
            throw new CustomException(ErrorCode.MISSING_REQUIRED_FIELDS);
        }

        Long memberId = userDetails.getUserId();
        personalScheduleService.updatePersonalSchedule(scheduleId, requestDto, memberId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/personal/{memberId}")
    public ResponseEntity<List<ScheduleDetailResponseDto>> getPersonalSchedulesByMemberId(
            @Parameter(description = "조회할 멤버의 ID") @PathVariable Long memberId) {
        validateToken();  // 토큰 검증

        if (memberId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_PARAMETERS);
        }

        List<ScheduleDetailResponseDto> response = personalScheduleService.getPersonalSchedulesByMemberId(memberId);
        if (response == null || response.isEmpty()) {
            throw new CustomException(ErrorCode.NOT_EXIST_DATA);
        }
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/personal/{scheduleId}")
    public ResponseEntity<Map<String, Boolean>> deletePersonalSchedule(
            @Parameter(description = "삭제할 일정의 ID") @PathVariable Long scheduleId) {
        CustomUserDetails userDetails = validateToken();
        
        if (scheduleId == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_PARAMETERS);
        }

        Long memberId = userDetails.getUserId();
        personalScheduleService.deletePersonalSchedule(scheduleId, memberId);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("success", true);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}