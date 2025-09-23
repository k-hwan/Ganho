package com.ssafy.ganhoho.domain.member.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberInfoResponse {
    private Long memberId;
    private String loginId;
    private String name;
    private String hospital;
    private String ward;
    private Double hospitalLat;
    private Double hospitalLng;
    private String appFcmToken;
    private String watchFcmToken;
}
