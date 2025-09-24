package com.ssafy.ganhoho.domain.medicine.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadImageResponse {
    private boolean success;
    private Map<String, Object> aiResult;
    private List<MedicineDetailDto> medicineInfo;
    private String detectedName;
}
