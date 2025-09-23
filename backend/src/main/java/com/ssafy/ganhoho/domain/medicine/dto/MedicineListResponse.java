package com.ssafy.ganhoho.domain.medicine.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineListResponse {
    private List<MedicineDetailDto> items;
}
