package com.ssafy.ganhoho.global.docs.schedule;

import com.ssafy.ganhoho.domain.schedule.dto.WorkScheduleRequestDto;
import com.ssafy.ganhoho.domain.schedule.dto.WorkScheduleResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "WorkSchedule", description = "근무 일정 API")
@SecurityRequirement(name = "bearer-jwt")
public interface WorkScheduleDocs {

    @Operation(summary = "근무 일정 조회", description = "인증된 사용자의 전체 근무 일정 조회")
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
                                    "scheduleTitle": "주간근무",
                                    "scheduleColor": "#FF5733",
                                    "isTimeSet": true
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
    ResponseEntity<List<WorkScheduleResponseDto>> getWorkSchedules();

    @Operation(summary = "특정 멤버의 근무 일정 조회", description = "특정 멤버의 전체 근무 일정 조회")
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
                                    "scheduleTitle": "주간근무",
                                    "scheduleColor": "#FF5733",
                                    "isTimeSet": true
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
    ResponseEntity<List<WorkScheduleResponseDto>> getWorkSchedulesByMemberId(
            @Parameter(description = "조회할 멤버의 ID") @PathVariable Long memberId);

    @Operation(summary = "근무 일정 수정", description = "특정 근무 일정을 수정")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일정 수정 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                        {
                            "success": true
                        }
                        """))),
            @ApiResponse(responseCode = "400", description = "workScheduleId가 null인 경우",
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
            @ApiResponse(responseCode = "404", description = "해당 workScheduleId의 일정이 없는 경우",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                        {
                            "status": "404",
                            "message": "데이터가 존재하지 않습니다."
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
    ResponseEntity<WorkScheduleResponseDto> updateWorkSchedule(
            @Parameter(description = "수정할 일정의 ID") @PathVariable Long workScheduleId,
            @Parameter(description = "수정할 일정 정보", required = true,
                    schema = @Schema(example = """
                    {
                        "scheduleTitle": "수정된 근무",
                        "startDt": "2024-03-21T09:00:00",
                        "endDt": "2024-03-21T18:00:00",
                        "scheduleColor": "#FF5733",
                        "isTimeSet": true
                    }
                    """))
            @RequestBody WorkScheduleRequestDto requestDto);

    @Operation(summary = "근무 일정 생성", description = "새로운 근무 일정 추가")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "일정 생성 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = """
                        {
                            "data": [
                                {
                                    "workScheduleDetailId": 1,
                                    "workType": "D",
                                    "workDate": "2024-02-01T09:00:00"
                                },
                                {
                                    "workScheduleDetailId": 1,
                                    "workType": "D",
                                    "workDate": "2024-02-02T09:00:00"
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
    ResponseEntity<List<WorkScheduleResponseDto>> addWorkSchedules(
            @Parameter(description = "추가할 일정 정보", required = true,
                    schema = @Schema(example = """
                    [
                        {
                            "workScheduleDetailId": 1,
                            "workType": "D",
                            "workDate": "2024-02-01T09:00:00"
                        },
                        {
                            "workScheduleDetailId": 1,
                            "workType": "D",
                            "workDate": "2024-02-02T09:00:00"
                        }
                    ]
                    """))
            @RequestBody List<WorkScheduleRequestDto> requestDtos);
}


