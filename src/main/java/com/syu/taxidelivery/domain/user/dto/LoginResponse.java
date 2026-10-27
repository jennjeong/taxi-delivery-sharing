package com.syu.taxidelivery.domain.user.dto;

import com.syu.taxidelivery.domain.user.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [로그인 성공 응답 DTO]
 * 세션 인증 성공 정보 및 인증된 서비스 화면 이동 경로를 담습니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private Long userId;
    private String name;
    private String schoolEmail;
    private Role role;
    private String redirectUrl; // 로그인 성공 후 이동할 서비스 화면 경로 (예: /service/main)
    private String accessToken; // (선택적) JWT 토큰 병행 지원용
}
