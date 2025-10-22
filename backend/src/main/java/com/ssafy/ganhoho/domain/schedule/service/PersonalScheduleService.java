package com.ssafy.ganhoho.domain.schedule.service;

import com.ssafy.ganhoho.domain.schedule.dto.PersonalScheduleRequestDto;
import com.ssafy.ganhoho.domain.schedule.dto.PersonalScheduleResponseDto;
import com.ssafy.ganhoho.domain.schedule.dto.ScheduleDetailResponseDto;
import com.ssafy.ganhoho.domain.schedule.entity.PersonalSchedule;
import com.ssafy.ganhoho.domain.schedule.entity.ScheduleDetail;
import com.ssafy.ganhoho.domain.schedule.repository.PersonalScheduleRepository;
import com.ssafy.ganhoho.domain.schedule.repository.ScheduleDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import com.ssafy.ganhoho.global.error.CustomException;
import com.ssafy.ganhoho.global.constant.ErrorCode;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PersonalScheduleService {

    private final PersonalScheduleRepository personalScheduleRepository;
    private final ScheduleDetailRepository scheduleDetailRepository;

    public PersonalScheduleResponseDto addPersonalSchedule(PersonalScheduleRequestDto requestDto, Long memberId) {
        PersonalSchedule schedule = new PersonalSchedule();
        schedule.setMemberId(memberId);
        schedule.setIsPublic(requestDto.getIsPublic() != null ? requestDto.getIsPublic() : false);
        personalScheduleRepository.save(schedule);

        // ScheduleDetail 생성 및 저장
        ScheduleDetail detail = ScheduleDetail.builder()
                .personalSchedule(schedule)
                .startDt(requestDto.getStartDt() != null ? requestDto.getStartDt() : LocalDateTime.now())
                .endDt(requestDto.getEndDt() != null ? requestDto.getEndDt() : LocalDateTime.now())
                .scheduleTitle(requestDto.getScheduleTitle())
                .scheduleColor(requestDto.getScheduleColor())
                .isTimeSet(requestDto.getIsTimeSet() != null ? requestDto.getIsTimeSet() : false)
                .build();

        schedule.getScheduleDetails().add(detail);
        Long scheduleDetailId = personalScheduleRepository.save(schedule).getScheduleId();

        return new PersonalScheduleResponseDto(schedule.getScheduleId(), memberId, List.of(
                new PersonalScheduleResponseDto.ScheduleDetailDto(
                        scheduleDetailId,
                        detail.getStartDt(),
                        detail.getEndDt(),
                        detail.getScheduleTitle(),
                        detail.getScheduleColor(),
                        detail.getIsTimeSet(),
                        schedule.getIsPublic()
                )
        ));
    }

    public List<PersonalScheduleResponseDto> getPersonalSchedules(Long memberId) {
        List<PersonalSchedule> schedules = personalScheduleRepository.findByMemberId(memberId);

        return schedules.stream()
                .flatMap(schedule -> schedule.getScheduleDetails().stream()
                        .map(detail -> new PersonalScheduleResponseDto(
                                detail.getPersonalSchedule().getScheduleId(),
                                schedule.getMemberId(),
                                List.of(new PersonalScheduleResponseDto.ScheduleDetailDto(
                                        detail.getDetailId(),
                                        detail.getStartDt(),
                                        detail.getEndDt(),
                                        detail.getScheduleTitle(),
                                        detail.getScheduleColor(),
                                        detail.getIsTimeSet(),
                                        schedule.getIsPublic()
                                ))
                        )))
                .collect(Collectors.toList());
    }

    public PersonalScheduleResponseDto updatePersonalSchedule(Long scheduleId, PersonalScheduleRequestDto requestDto, Long memberId) {
        // 1. 일정 존재 여부 확인
        PersonalSchedule schedule = personalScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_EXIST_DATA));

        // 2. 권한 확인
        if (!schedule.getMemberId().equals(memberId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        try {
            // 3. isPublic 업데이트
            schedule.setIsPublic(requestDto.getIsPublic());

            // 4. 기존 ScheduleDetail 업데이트
            if (!schedule.getScheduleDetails().isEmpty()) {
                log.warn("schedule is not empty : {}",schedule.getScheduleDetails());
                ScheduleDetail existingDetail = schedule.getScheduleDetails().get(0);
                existingDetail.setStartDt(requestDto.getStartDt());
                existingDetail.setEndDt(requestDto.getEndDt());
                existingDetail.setScheduleTitle(requestDto.getScheduleTitle());
                existingDetail.setScheduleColor(requestDto.getScheduleColor());
                existingDetail.setIsTimeSet(requestDto.getIsTimeSet());

            } else {
                // 만약 ScheduleDetail이 없다면 새로 생성
                ScheduleDetail newDetail = ScheduleDetail.builder()
                    .personalSchedule(schedule)
                    .startDt(requestDto.getStartDt())
                    .endDt(requestDto.getEndDt())
                    .scheduleTitle(requestDto.getScheduleTitle())
                    .scheduleColor(requestDto.getScheduleColor())
                    .isTimeSet(requestDto.getIsTimeSet())
                    .build();
                schedule.getScheduleDetails().add(newDetail);
            }

            personalScheduleRepository.save(schedule);

            // 5. 응답 생성
            ScheduleDetail updatedDetail = schedule.getScheduleDetails().get(0);
            return new PersonalScheduleResponseDto(schedule.getScheduleId(), memberId, List.of(
                new PersonalScheduleResponseDto.ScheduleDetailDto(
                    updatedDetail.getDetailId(),
                    updatedDetail.getStartDt(),
                    updatedDetail.getEndDt(),
                    updatedDetail.getScheduleTitle(),
                    updatedDetail.getScheduleColor(),
                    updatedDetail.getIsTimeSet(),
                    schedule.getIsPublic()
                )
            ));
            
        } catch (DataIntegrityViolationException e) {
            log.error("데이터 무결성 위반: {}", e.getMessage());
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
        } catch (Exception e) {
            log.error("일정 업데이트 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.SERVER_ERROR);
        }
    }

    private void validateScheduleRequest(PersonalScheduleRequestDto requestDto) {
        if (requestDto.getScheduleTitle() == null || requestDto.getScheduleTitle().trim().isEmpty()) {
            throw new CustomException(ErrorCode.MISSING_REQUIRED_FIELDS);
        }
        if (requestDto.getStartDt() == null || requestDto.getEndDt() == null) {
            throw new CustomException(ErrorCode.MISSING_REQUIRED_FIELDS);
        }
        if (requestDto.getStartDt().isAfter(requestDto.getEndDt())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_DATA);
        }
    }

    public void deletePersonalSchedule(Long scheduleId, Long memberId) {
        PersonalSchedule schedule = personalScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_EXIST_DATA));

        if (!schedule.getMemberId().equals(memberId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        try {
            personalScheduleRepository.delete(schedule);
        } catch (Exception e) {
            log.error("일정 삭제 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.SERVER_ERROR);
        }
    }

    public List<ScheduleDetailResponseDto> getPersonalSchedulesByMemberId(Long memberId) {
        List<PersonalSchedule> schedules = personalScheduleRepository.findByMemberId(memberId);

        List<ScheduleDetailResponseDto> list = schedules.stream()
                .filter(schedule -> Boolean.TRUE.equals(schedule.getIsPublic()))
                .flatMap(schedule -> schedule.getScheduleDetails().stream()
                        .map(detail -> ScheduleDetailResponseDto.builder()
                                .scheduleId(schedule.getScheduleId())
                                .detailId(detail.getDetailId())
                                .startDt(detail.getStartDt())
                                .endDt(detail.getEndDt())
                                .scheduleTitle(detail.getScheduleTitle())
                                .scheduleColor(detail.getScheduleColor())
                                .isPublic(schedule.getIsPublic())
                                .isTimeSet(detail.getIsTimeSet())
                                .build()))
                .collect(Collectors.toList());

        return list;
    }

    public List<ScheduleDetailResponseDto> getFormattedPersonalSchedules(Long memberId) {
        List<PersonalScheduleResponseDto> schedules = getPersonalSchedules(memberId);

        List<ScheduleDetailResponseDto> list = schedules.stream()
                .flatMap(schedule -> schedule.getDetails().stream()
                        .map(detail -> ScheduleDetailResponseDto.builder()
                                .scheduleId(schedule.getScheduleId())
                                .detailId(detail.getDetailId())
                                .startDt(detail.getStartDt())
                                .endDt(detail.getEndDt())
                                .scheduleTitle(detail.getScheduleTitle())
                                .scheduleColor(detail.getScheduleColor())
                                .isPublic(detail.getIsPublic())
                                .isTimeSet(detail.getIsTimeSet())
                                .build()))
                .collect(Collectors.toList());

        return list;
    }

    public PersonalSchedule getSchedule(Long scheduleId) {
        Optional<PersonalSchedule> schedule = personalScheduleRepository.findById(scheduleId);
        return schedule.orElseThrow(() -> new RuntimeException("Schedule not found"));
    }
}