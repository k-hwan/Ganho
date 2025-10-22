package com.ssafy.ganhoho.domain.schedule.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDetailResponseDto {
    private Long scheduleId;
    private Long detailId;
    private LocalDateTime startDt;
    private LocalDateTime endDt;
    private String scheduleTitle;
    private String scheduleColor;
    private Boolean isPublic;
    private Boolean isTimeSet;
}


