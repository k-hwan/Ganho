package com.ssafy.ganhoho.domain.schedule.repository;

import com.ssafy.ganhoho.domain.schedule.entity.PersonalSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonalScheduleRepository extends JpaRepository<PersonalSchedule, Long> {
    List<PersonalSchedule> findByMemberId(Long memberId);
}
