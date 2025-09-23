package com.ssafy.ganhoho.domain.medicine.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.ganhoho.domain.medicine.dto.MedicineDetailDto;
import com.ssafy.ganhoho.domain.medicine.dto.MedicineListResponse;
import com.ssafy.ganhoho.domain.medicine.dto.UploadImageResponse;

public interface MedicineService {
    ResponseEntity<MedicineDetailDto> getMedicineById(String itemSeq);
    ResponseEntity<MedicineListResponse> searchMedicineByName(String itemName);
    ResponseEntity<UploadImageResponse> uploadMedicineImage(MultipartFile imageFile);
}
