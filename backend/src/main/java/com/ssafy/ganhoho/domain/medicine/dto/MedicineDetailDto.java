package com.ssafy.ganhoho.domain.medicine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineDetailDto {
    private String ITEM_SEQ;
    private String ITEM_NAME;
    private String ENTP_NAME;
    private String ETC_OTC_CODE;
    private String CHART;
    private String STORAGE_METHOD;
    private String VALID_TERM;
    private String NEWDRUG_CLASS_NAME;
    private String EE_DOC_DATA;
    private String UD_DOC_DATA;
    private String NB_DOC_DATA;
    private String PN_DOC_DATA;
    private String ITEM_IMAGE;
}
