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
import com.ssafy.ganhoho.global.error.ErrorResponse;
import com.ssafy.ganhoho.global.constant.ErrorCode;
import com.ssafy.ganhoho.global.error.CustomException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;

@Slf4j
@Tag(name = "PersonalSchedule", description = "개인 일정 API")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class PersonalScheduleController {

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

    @Operation(summary = "개인 일정 추가", description = "새로운 개인 일정 추가")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "일정 생성 성공",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "scheduleId": 1,
                            "memberId": 1,
                            "details": [
                                {
                                    "detailId": 1,
                                    "startDt": "2024-03-21T09:00:00",
                                    "endDt": "2024-03-21T18:00:00",
                                    "scheduleTitle": "일정 제목",
                                    "scheduleColor": "#FF5733",
                                    "isTimeSet": false,
                                    "isPublic": true
                                }
                            ]
                        }
                        """))),
            @ApiResponse(responseCode = "400", description = "필수 항목이 누락되었습니다.",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "400",
                            "message": "필수 항목이 누락되었습니다."
                        }
                        """))),
            @ApiResponse(responseCode = "401", description = "토큰이 없는 경우",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "401",
                            "message": "유효하지 않은 토큰입니다."
                        }
                        """))),
            @ApiResponse(responseCode = "401", description = "토큰이 만료된 경우",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "401",
                            "message": "만료된 토큰입니다."
                        }
                        """))),
            @ApiResponse(responseCode = "401", description = "토큰에서 userId를 추출할 수 없는 경우",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "401",
                            "message": "필수 사용자 데이터가 누락되었습니다."
                        }
                        """))),
            @ApiResponse(responseCode = "500", description = "서버에서 예외가 발생한 경우",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "500",
                            "message": "서버 내부 오류가 발생했습니다."
                        }
                        """))),
    })
    @PostMapping("/personal")
    public ResponseEntity<PersonalScheduleResponseDto> addPersonalSchedule(
            @Parameter(description = "추가할 일정 정보", required = true,
                schema = @Schema(example = """
                    {
                        "scheduleTitle": "새로운 일정",
                        "startDt": "2024-03-21T09:00:00",
                        "endDt": "2024-03-21T18:00:00",
                        "scheduleColor": "#FF5733",
                        "isTimeSet": false,
                        "isPublic": true
                    }
                    """))
            @RequestBody PersonalScheduleRequestDto requestDto) {
        CustomUserDetails userDetails = validateToken();
        
        if (requestDto == null) {
            throw new CustomException(ErrorCode.MISSING_REQUIRED_FIELDS);
        }

        Long memberId = userDetails.getUserId();
        PersonalScheduleResponseDto createdSchedule = personalScheduleService.addPersonalSchedule(requestDto, memberId);
        return new ResponseEntity<>(createdSchedule, HttpStatus.CREATED);
    }

    @Operation(summary = "개인 일정 조회", description = "인증된 사용자의 전체 개인 일정 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 조회 성공",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "data": [
                                {
                                    "scheduleId": 1,
                                    "startDt": "2024-03-21T09:00:00",
                                    "endDt": "2024-03-21T18:00:00",
                                    "title": "일정 제목",
                                    "color": "#FF5733",
                                    "isPublic": true,
                                    "isTimeSet": false
                                }
                            ]
                        }
                        """))),
            @ApiResponse(responseCode = "200", description = "조회된 데이터가 없습니다.",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "200",
                            "message": "데이터가 존재하지 않습니다."
                        }
                        """))),
            @ApiResponse(responseCode = "401", description = "토큰이 없는 경우",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "401",
                            "message": "유효하지 않은 토큰입니다."
                        }
                        """))),
            @ApiResponse(responseCode = "401", description = "토큰이 만료된 경우",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "401",
                            "message": "만료된 토큰입니다."
                        }
                        """))),
            @ApiResponse(responseCode = "401", description = "토큰에서 userId를 추출할 수 없는 경우",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "401",
                            "message": "필수 사용자 데이터가 누락되었습니다."
                        }
                        """))),
            @ApiResponse(responseCode = "500", description = "서버에서 예외가 발생한 경우",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "500",
                            "message": "서버 내부 오류가 발생했습니다."
                        }
                        """))),
    })
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

    @Operation(summary = "개인 일정 수정", description = "특정 개인 일정을 수정")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 수정 성공",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "success": true
                        }
                        """))),
            @ApiResponse(responseCode = "400", description = "scheduleId가 null인 경우",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "400",
                            "message": "잘못된 요청 파라미터입니다."
                        }
                        """))),
            @ApiResponse(responseCode = "400", description = "requestDto가 null이거나 필수 필드 누락",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "400",
                            "message": "필수 항목이 누락되었습니다."
                        }
                        """))),
            @ApiResponse(responseCode = "401", description = "토큰이 없거나 유효하지 않은 경우",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "401",
                            "message": "인증되지 않은 요청입니다."
                        }
                        """))),
            @ApiResponse(responseCode = "404", description = "해당 scheduleId의 일정이 없는 경우",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "404",
                            "message": "데이터가 존재하지 않습니다."
                        }
                        """))),
            @ApiResponse(responseCode = "409", description = "데이터 무결성 위반 시",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "409",
                            "message": "이미 존재하는 리소스입니다."
                        }
                        """))),
            @ApiResponse(responseCode = "500", description = "기타 서버 오류 발생 시",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "500",
                            "message": "서버 내부 오류가 발생했습니다."
                        }
                        """))),
    })
    @PutMapping("/personal/{scheduleId}")
    public ResponseEntity<Map<String, Boolean>> updatePersonalSchedule(
            @Parameter(description = "수정할 일정의 ID") @PathVariable Long scheduleId,
            @Parameter(description = "수정할 일정 정보", required = true,
                schema = @Schema(example = """
                    {
                        "scheduleTitle": "수정된 일정",
                        "startDt": "2024-03-21T09:00:00",
                        "endDt": "2024-03-21T18:00:00",
                        "scheduleColor": "#FF5733",
                        "isTimeSet": false,
                        "isPublic": true
                    }
                    """))
            @RequestBody PersonalScheduleRequestDto requestDto) {
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

    @Operation(summary = "특정 멤버의 일정 조회", description = "특정 멤버의 전체 일정 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 조회 성공",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "data": [
                                {
                                    "scheduleId": 1,
                                    "startDt": "2024-03-21T09:00:00",
                                    "endDt": "2024-03-21T18:00:00",
                                    "title": "일정 제목",
                                    "color": "#FF5733",
                                    "isPublic": true,
                                    "isTimeSet": false
                                }
                            ]
                        }
                        """))),
            @ApiResponse(responseCode = "200", description = "조회된 데이터가 없습니다.",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "200",
                            "message": "데이터가 존재하지 않습니다."
                        }
                        """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "400",
                            "message": "잘못된 요청 파라미터입니다."
                        }
                        """))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청입니다.",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "401",
                            "message": "인증되지 않은 요청입니다."
                        }
                        """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원입니다.",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "404",
                            "message": "데이터가 존재하지 않습니다."
                        }
                        """))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류가 발생했습니다.",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "500",
                            "message": "서버 내부 오류가 발생했습니다."
                        }
                        """))),
    })
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

    @Operation(summary = "개인 일정 삭제", description = "특정 개인 일정을 삭제")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 삭제 성공",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "success": true
                        }
                        """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "400",
                            "message": "잘못된 요청 파라미터입니다."
                        }
                        """))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 요청",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "401",
                            "message": "인증되지 않은 요청입니다."
                        }
                        """))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 일정",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                        {
                            "status": "404",
                            "message": "데이터가 존재하지 않습니다."
                        }
                        """))),
    })
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