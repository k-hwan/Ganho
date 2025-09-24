package com.ssafy.ganhoho.global.auth.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JWTToken {
    private String grantType;
    private String accessToken;
    private String refreshToken;
}
