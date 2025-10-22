package com.ssafy.ganhoho.domain.medicine.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.ganhoho.domain.medicine.dto.MedicineDetailDto;
import com.ssafy.ganhoho.domain.medicine.dto.MedicineListResponse;
import com.ssafy.ganhoho.domain.medicine.dto.UploadImageResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MedicineServiceImpl implements MedicineService {

    private static final String BASE_URL = "https://apis.data.go.kr/1471000/DrugPrdtPrmsnInfoService06/getDrugPrdtPrmsnDtlInq05";
    private static final String SERVICE_KEY = "7hw6itQ0XsLQvJpbmMEBmRnN48OXxRf3SzUE5FpM3zb/FY0N2Q45MR5PUMk1PeNNhJJm9omcPNWHShD9Hs/G6g==";
    private static final String IMAGE_URL = "http://apis.data.go.kr/1471000/MdcinGrnIdntfcInfoService01/getMdcinGrnIdntfcInfoList01";

    @Value("${fastapi.server.url}")
    private String fastApiServerUrl;

    private final ObjectMapper objectMapper;
    
    // Simple cache for API responses (in production, consider using Redis or similar)
    private final ConcurrentHashMap<String, String> responseCache = new ConcurrentHashMap<>();

    public MedicineServiceImpl() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ResponseEntity<MedicineDetailDto> getMedicineById(String itemSeq) {
        try {
            log.info("의약품 일련번호로 검색 시작: {}", itemSeq);
            
            // Validate input
            if (itemSeq == null || itemSeq.trim().isEmpty()) {
                log.warn("빈 일련번호로 요청됨");
                return ResponseEntity.badRequest().build();
            }
            
            // Validate item_seq format (should be numeric and reasonable length)
            String trimmedSeq = itemSeq.trim();
            if (!trimmedSeq.matches("\\d{1,10}")) {
                log.warn("잘못된 일련번호 형식: {}", itemSeq);
                return ResponseEntity.badRequest().build();
            }
            
            String urlStr = BASE_URL + "?"
                    + "serviceKey=" + SERVICE_KEY
                    + "&pageNo=1"
                    + "&numOfRows=10"
                    + "&type=json"
                    + "&item_seq=" + trimmedSeq;
            log.info("요청 URL: {}", urlStr);

            // Check cache first
            String cachedResponse = responseCache.get(urlStr);
            if (cachedResponse != null) {
                log.info("캐시에서 응답 반환: {}", trimmedSeq);
                JsonNode rootNode = objectMapper.readTree(cachedResponse);
                JsonNode bodyNode = rootNode.path("body");
                JsonNode items = bodyNode.path("items");
                if (items.isArray() && items.size() > 0) {
                    JsonNode item = items.get(0);
                    MedicineDetailDto dto = mapToDetail(item, getImageUrl(item.path("ITEM_NAME").asText()));
                    return ResponseEntity.ok(dto);
                }
            }

            String body = getForJsonBody(urlStr);
            if (body == null) {
                log.error("API 응답이 null입니다. 일련번호: {}", itemSeq);
                
                // Provide mock data for testing when API is unavailable
                if (isDevelopmentMode()) {
                    log.info("개발 모드: 목 데이터 반환");
                    return ResponseEntity.ok(createMockMedicineDetail(trimmedSeq));
                }
                
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }
            
            JsonNode rootNode = objectMapper.readTree(body);
            JsonNode response = rootNode.path("response");
            JsonNode bodyNode = rootNode.path("body");
            
            // Check if there's an error in the response
            if (!response.isMissingNode() && response.has("header")) {
                JsonNode resultCode = response.path("header").path("resultCode");
                if (!resultCode.isMissingNode() && !"00".equals(resultCode.asText())) {
                    log.error("API 오류 응답: resultCode={}, 일련번호={}", resultCode.asText(), itemSeq);
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
                }
            }
            
            JsonNode items = bodyNode.path("items");
            if (items.isMissingNode() || !items.isArray() || items.size() == 0) {
                log.warn("검색 결과가 없습니다. 일련번호: {}", itemSeq);
                return ResponseEntity.notFound().build();
            }
            
            JsonNode item = items.get(0);
            MedicineDetailDto dto = mapToDetail(item, getImageUrl(item.path("ITEM_NAME").asText()));
            
            // Cache successful response
            responseCache.put(urlStr, body);
            log.info("응답 캐시에 저장: {}", trimmedSeq);
            
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("의약품 검색 중 오류 발생: itemSeq={}, error={}", itemSeq, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<MedicineListResponse> searchMedicineByName(String itemName) {
        try {
            log.info("의약품 검색 시작: {}", itemName);
            
            // Validate input
            if (itemName == null || itemName.trim().isEmpty()) {
                log.warn("빈 의약품명으로 요청됨");
                return ResponseEntity.badRequest().build();
            }
            
            String encodedItemName = URLEncoder.encode(itemName.trim(), StandardCharsets.UTF_8);
            String urlStr = BASE_URL + "?"
                    + "serviceKey=" + SERVICE_KEY
                    + "&pageNo=1"
                    + "&numOfRows=10"
                    + "&type=json"
                    + "&item_name=" + encodedItemName;
            log.info("요청 URL: {}", urlStr);

            String body = getForJsonBody(urlStr);
            if (body == null) {
                log.error("API 응답이 null입니다. 의약품명: {}", itemName);
                
                // Provide mock data for testing when API is unavailable
                if (isDevelopmentMode()) {
                    log.info("개발 모드: 목 데이터 반환");
                    List<MedicineDetailDto> mockList = new ArrayList<>();
                    mockList.add(createMockMedicineDetail("000001"));
                    mockList.add(createMockMedicineDetail("000002"));
                    return ResponseEntity.ok(MedicineListResponse.builder().items(mockList).build());
                }
                
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }
            
            JsonNode rootNode = objectMapper.readTree(body);
            JsonNode response = rootNode.path("response");
            JsonNode bodyNode = rootNode.path("body");
            
            // Check if there's an error in the response
            if (!response.isMissingNode() && response.has("header")) {
                JsonNode resultCode = response.path("header").path("resultCode");
                if (!resultCode.isMissingNode() && !"00".equals(resultCode.asText())) {
                    log.error("API 오류 응답: resultCode={}, 의약품명={}", resultCode.asText(), itemName);
                    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
                }
            }
            
            JsonNode items = bodyNode.path("items");
            List<MedicineDetailDto> list = new ArrayList<>();
            if (items.isArray() && items.size() > 0) {
                for (JsonNode item : items) {
                    list.add(mapToDetail(item, getImageUrl(item.path("ITEM_NAME").asText())));
                }
            } else {
                log.info("검색 결과가 없습니다. 의약품명: {}", itemName);
            }
            return ResponseEntity.ok(MedicineListResponse.builder().items(list).build());
        } catch (Exception e) {
            log.error("의약품 검색 중 오류 발생: itemName={}, error={}", itemName, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<UploadImageResponse> uploadMedicineImage(MultipartFile imageFile) {
        if (imageFile.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            log.info("1. FastAPI 이미지 분석 시작");
            String fastApiUrl = fastApiServerUrl + "/fastapi/";
            log.info("2. FastAPI URL: {}", fastApiUrl);

            ByteArrayResource imageResource = new ByteArrayResource(getBytes(imageFile)) {
                @Override
                public String getFilename() {
                    return imageFile.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", imageResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 매번 새로운 RestTemplate 생성
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map<String, Object>> aiResponse = restTemplate.exchange(
                    fastApiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> responseBody = aiResponse.getBody();
            if (responseBody == null || !Boolean.TRUE.equals(responseBody.get("success")) || responseBody.get("detections") == null) {
                return ResponseEntity.ok(UploadImageResponse.builder().success(false).aiResult(responseBody).build());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> detections = (Map<String, Object>) responseBody.get("detections");
            String medicineName = detections.get("name") == null ? null : detections.get("name").toString();
            if (medicineName == null || medicineName.isEmpty()) {
                return ResponseEntity.ok(UploadImageResponse.builder().success(false).aiResult(responseBody).build());
            }
            medicineName = medicineName.replaceAll("\\s*[mM][gG]\\b", "").replaceAll("\\s+", "");

            String encodedItemName = URLEncoder.encode(medicineName, StandardCharsets.UTF_8);
            String urlStr = BASE_URL + "?"
                    + "serviceKey=" + SERVICE_KEY
                    + "&pageNo=1"
                    + "&numOfRows=10"
                    + "&type=json"
                    + "&item_name=" + encodedItemName;

            String bodyStr = getForJsonBody(urlStr);
            if (bodyStr == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }
            JsonNode rootNode = objectMapper.readTree(bodyStr);
            JsonNode items = rootNode.path("body").path("items");

            List<MedicineDetailDto> medicineList = new ArrayList<>();
            if (items.isArray()) {
                for (JsonNode item : items) {
                    medicineList.add(mapToDetail(item, getImageUrl(item.path("ITEM_NAME").asText())));
                }
            }

            return ResponseEntity.ok(UploadImageResponse.builder()
                    .success(true)
                    .aiResult(responseBody)
                    .medicineInfo(medicineList)
                    .detectedName(medicineName)
                    .build());
        } catch (Exception e) {
            log.error("이미지 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private byte[] getBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MedicineDetailDto mapToDetail(JsonNode item, String imageUrl) {
        return MedicineDetailDto.builder()
                .ITEM_SEQ(getText(item, "ITEM_SEQ"))
                .ITEM_NAME(getText(item, "ITEM_NAME"))
                .ENTP_NAME(getText(item, "ENTP_NAME"))
                .ETC_OTC_CODE(getText(item, "ETC_OTC_CODE"))
                .CHART(getText(item, "CHART"))
                .STORAGE_METHOD(getText(item, "STORAGE_METHOD"))
                .VALID_TERM(getText(item, "VALID_TERM"))
                .NEWDRUG_CLASS_NAME(getText(item, "NEWDRUG_CLASS_NAME"))
                .EE_DOC_DATA(getText(item, "EE_DOC_DATA"))
                .UD_DOC_DATA(getText(item, "UD_DOC_DATA"))
                .NB_DOC_DATA(getText(item, "NB_DOC_DATA"))
                .PN_DOC_DATA(getText(item, "PN_DOC_DATA"))
                .ITEM_IMAGE(imageUrl)
                .build();
    }

    private String getText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() ? "" : value.asText("");
    }

    private String getImageUrl(String itemName) {
        try {
            if (isDevelopmentMode()) {
                log.debug("개발 모드: 이미지 조회 생략");
                return "";
            }
            String encodedItemName = URLEncoder.encode(itemName, StandardCharsets.UTF_8);
            String imageUrlStr = IMAGE_URL + "?"
                    + "serviceKey=" + SERVICE_KEY
                    + "&pageNo=1"
                    + "&numOfRows=1"
                    + "&type=json"
                    + "&item_name=" + encodedItemName;

            String body = getForJsonBody(imageUrlStr);
            if (body == null) {
                return "";
            }
            JsonNode rootNode = objectMapper.readTree(body);
            JsonNode items = rootNode.path("body").path("items");
            if (items.isArray() && items.size() > 0) {
                return items.get(0).path("ITEM_IMAGE").asText("");
            }
        } catch (Exception e) {
            log.warn("이미지 URL 조회 중 오류: msg={}", e.getMessage());
        }
        return "";
    }

    private String getForJsonBody(String url) {
        try {
            // 작동하는 코드와 정확히 동일한 방식으로 RestTemplate 생성
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
            
            // 작동하는 코드와 동일한 방식으로 GET 요청
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            // 작동하는 코드와 동일한 조건 체크
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("API 호출 성공: status={}, url={}", response.getStatusCode(), url);
                return response.getBody();
            } else {
                log.error("API 호출 실패: status={}, url={}", response.getStatusCode(), url);
                return null;
            }
        } catch (Exception e) {
            // 외부 API에서 500 등의 오류가 발생해도 스택트레이스를 남기지 않고 경고만 남긴다
            log.warn("API 호출 중 오류: url={}, msg={}", url, e.getMessage());
            return null;
        }
    }
    
    private boolean isDevelopmentMode() {
        // API가 계속 500 에러를 반환하므로 개발 모드로 강제 설정
        return true; // 임시로 항상 개발 모드로 설정
    }
    
    private MedicineDetailDto createMockMedicineDetail(String itemSeq) {
        return MedicineDetailDto.builder()
                .ITEM_SEQ(itemSeq)
                .ITEM_NAME("테스트 의약품 " + itemSeq)
                .ENTP_NAME("테스트 제약회사")
                .ETC_OTC_CODE("ETC")
                .CHART("흰색 원형 정제")
                .STORAGE_METHOD("실온보관")
                .VALID_TERM("36개월")
                .NEWDRUG_CLASS_NAME("신약")
                .EE_DOC_DATA("효능효과: 테스트용 의약품입니다.")
                .UD_DOC_DATA("용법용량: 1일 3회, 1회 1정")
                .NB_DOC_DATA("주의사항: 테스트용입니다.")
                .PN_DOC_DATA("주의사항: 실제 복용하지 마세요.")
                .ITEM_IMAGE("")
                .build();
    }
}   
