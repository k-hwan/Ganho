package com.ssafy.ganhoho.domain.auth.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String loginId;
    private String password;
    private String fcmToken;
}