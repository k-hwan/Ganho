package com.ssafy.ganhoho.domain.medicine.controller;

import com.ssafy.ganhoho.domain.medicine.dto.MedicineDetailDto;
import com.ssafy.ganhoho.domain.medicine.dto.MedicineListResponse;
import com.ssafy.ganhoho.domain.medicine.dto.UploadImageResponse;
import com.ssafy.ganhoho.domain.medicine.service.MedicineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping("/api/medicines/{itemSeq}")
    public ResponseEntity<MedicineDetailDto> getMedicineById(@PathVariable String itemSeq) {
        return medicineService.getMedicineById(itemSeq);
    }

    @GetMapping("/api/medicines/search")
    public ResponseEntity<MedicineListResponse> searchMedicineByName(@RequestParam String itemName) {
        return medicineService.searchMedicineByName(itemName);
    }

    @PostMapping("/api/medicines/upload-image")
    public ResponseEntity<UploadImageResponse> uploadMedicineImage(@RequestParam("imageFile") MultipartFile imageFile) {
        return medicineService.uploadMedicineImage(imageFile);
    }
}
