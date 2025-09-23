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

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String encodedServiceKey;

    public MedicineServiceImpl() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        this.objectMapper = new ObjectMapper();
        this.encodedServiceKey = URLEncoder.encode(SERVICE_KEY, StandardCharsets.UTF_8);
    }

    @Override
    public ResponseEntity<MedicineDetailDto> getMedicineById(String itemSeq) {
        try {
            log.info("의약품 일련번호로 검색 시작: {}", itemSeq);
            String urlStr = BASE_URL + "?"
                    + "serviceKey=" + SERVICE_KEY
                    + "&pageNo=1"
                    + "&numOfRows=10"
                    + "&type=json"
                    + "&item_seq=" + itemSeq;
            log.info("요청 URL: {}", urlStr);

            String body = getForJsonBody(urlStr);
            if (body == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }
            JsonNode rootNode = objectMapper.readTree(body);
            JsonNode item = rootNode.path("body").path("items").get(0);
            if (item == null || item.isMissingNode()) {
                return ResponseEntity.notFound().build();
            }
            MedicineDetailDto dto = mapToDetail(item, getImageUrl(item.path("ITEM_NAME").asText()));
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("의약품 검색 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<MedicineListResponse> searchMedicineByName(String itemName) {
        try {
            log.info("의약품 검색 시작: {}", itemName);
            String encodedItemName = URLEncoder.encode(itemName, StandardCharsets.UTF_8);
            String urlStr = BASE_URL + "?"
                    + "serviceKey=" + SERVICE_KEY
                    + "&pageNo=1"
                    + "&numOfRows=10"
                    + "&type=json"
                    + "&item_name=" + encodedItemName;
            log.info("요청 URL: {}", urlStr);

            String body = getForJsonBody(urlStr);
            if (body == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }
            JsonNode rootNode = objectMapper.readTree(body);
            JsonNode items = rootNode.path("body").path("items");
            List<MedicineDetailDto> list = new ArrayList<>();
            if (items.isArray()) {
                for (JsonNode item : items) {
                    list.add(mapToDetail(item, getImageUrl(item.path("ITEM_NAME").asText())));
                }
            }
            return ResponseEntity.ok(MedicineListResponse.builder().items(list).build());
        } catch (Exception e) {
            log.error("의약품 검색 중 오류 발생: {}", e.getMessage(), e);
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
            log.error("이미지 URL 조회 중 오류 발생: {}", e.getMessage(), e);
        }
        return "";
    }

    private String getForJsonBody(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, MediaType.ALL_VALUE); // */*
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // helper lambda
        java.util.function.Function<String, String> tryFetch = (u) -> {
            ResponseEntity<String> r = restTemplate.exchange(u, HttpMethod.GET, entity, String.class);
            String b = r.getBody();
            if (b != null && !b.isBlank() && b.charAt(0) != '<') {
                return b;
            }
            log.warn("비JSON 응답 또는 빈 응답: status={}, length={} url={}", r.getStatusCode(), b == null ? -1 : b.length(), u);
            return null;
        };

        // 1) as-is
        String body = tryFetch.apply(url);
        if (body != null) return body;

        // 2) switch https->http
        if (url.startsWith("https://")) {
            String httpUrl = url.replaceFirst("https://", "http://");
            body = tryFetch.apply(httpUrl);
            if (body != null) return body;
        }

        // 3) toggle serviceKey encoding
        String rawKeyUrl = url.replace("serviceKey=" + encodedServiceKey, "serviceKey=" + SERVICE_KEY);
        if (!rawKeyUrl.equals(url)) {
            body = tryFetch.apply(rawKeyUrl);
            if (body != null) return body;
            if (rawKeyUrl.startsWith("https://")) {
                String httpUrl = rawKeyUrl.replaceFirst("https://", "http://");
                body = tryFetch.apply(httpUrl);
                if (body != null) return body;
            }
        } else {
            String encodedKeyUrl = url.replace("serviceKey=" + SERVICE_KEY, "serviceKey=" + encodedServiceKey);
            if (!encodedKeyUrl.equals(url)) {
                body = tryFetch.apply(encodedKeyUrl);
                if (body != null) return body;
                if (encodedKeyUrl.startsWith("https://")) {
                    String httpUrl = encodedKeyUrl.replaceFirst("https://", "http://");
                    body = tryFetch.apply(httpUrl);
                    if (body != null) return body;
                }
            }
        }

        // 4) append _type=json
        String withType = url.contains("_type=json") ? url : (url + "&_type=json");
        body = tryFetch.apply(withType);
        if (body != null) return body;
        if (withType.startsWith("https://")) {
            String httpUrl = withType.replaceFirst("https://", "http://");
            body = tryFetch.apply(httpUrl);
            if (body != null) return body;
        }

        return null;
    }
}
