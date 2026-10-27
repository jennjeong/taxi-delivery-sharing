package com.syu.taxidelivery.domain.user.controller;

import com.syu.taxidelivery.domain.user.dto.*;
import com.syu.taxidelivery.domain.user.entity.User;
import com.syu.taxidelivery.domain.user.service.LocalAuthService;
import com.syu.taxidelivery.domain.user.service.SchoolEmailAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * [학교 이메일 인증 및 세션 기반 로그인 컨트롤러]
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SchoolEmailAuthController {

    private final SchoolEmailAuthService schoolEmailAuthService;
    private final LocalAuthService localAuthService;

    /** 1. 회원가입용 인증코드 발송 API */
    @PostMapping("/school-email/send-code")
    public ResponseEntity<String> sendCode(@Valid @RequestBody SendMailCodeRequest request) {
        schoolEmailAuthService.sendVerificationCode(request);
        return ResponseEntity.ok("학교 이메일로 인증 코드가 발송되었습니다.");
    }

    /** 2. 회원가입용 인증코드 검증 API */
    @PostMapping("/school-email/verify-code")
    public ResponseEntity<String> verifyCode(@Valid @RequestBody VerifyMailCodeRequest request) {
        schoolEmailAuthService.verifyCode(request);
        return ResponseEntity.ok("학교 이메일 인증이 성공적으로 완료되었습니다.");
    }

    /** 3. 최종 회원가입 완료 API */
    @PostMapping("/school-email/register")
    public ResponseEntity<Long> register(@Valid @RequestBody SchoolRegisterRequest request) {
        User registeredUser = schoolEmailAuthService.registerWithSchoolEmail(request);
        return ResponseEntity.ok(registeredUser.getId());
    }

    /**
     * 4. 세션 기반 로그인 API
     * - 인증 성공 시 HttpSession을 발급하고 Spring SecurityContext에 등록
     * - 로그인 성공 후 인증된 서비스 화면 이동 경로(/service/main) 반환
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LocalLoginRequest loginRequest,
                                               HttpServletRequest request) {
        User user = localAuthService.login(loginRequest);

        // Security 권한(Role) 생성
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        // Authentication 객체 생성 및 SecurityContext 저장
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(user.getSchoolEmail(), null, authorities);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // 세션에 SecurityContext 영속화 (JSESSIONID 발급)
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        // 인증된 서비스 화면 이동 URL 및 사용자 정보 반환
        LoginResponse response = LoginResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .schoolEmail(user.getSchoolEmail())
                .role(user.getRole())
                .redirectUrl("/service/main") // 채택된 방식으로 로그인 성공 후 인증된 서비스 화면으로 이동
                .build();

        return ResponseEntity.ok(response);
    }

    /** 5. 비밀번호 재설정 - 1단계: 학교 이메일 재인증 코드 발송 */
    @PostMapping("/school-email/reset-password/send-code")
    public ResponseEntity<String> sendResetPasswordCode(@Valid @RequestBody SendMailCodeRequest request) {
        schoolEmailAuthService.sendResetPasswordCode(request);
        return ResponseEntity.ok("비밀번호 재설정 인증 코드가 발송되었습니다.");
    }

    /** 6. 비밀번호 재설정 - 2단계: 인증 코드 검증 */
    @PostMapping("/school-email/reset-password/verify-code")
    public ResponseEntity<String> verifyResetPasswordCode(@Valid @RequestBody VerifyMailCodeRequest request) {
        schoolEmailAuthService.verifyResetPasswordCode(request);
        return ResponseEntity.ok("비밀번호 재설정용 이메일 재인증이 완료되었습니다.");
    }

    /** 7. 비밀번호 재설정 - 3단계: 새 비밀번호 변경 및 계정 잠금 해제 */
    @PostMapping("/school-email/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        schoolEmailAuthService.resetPassword(request);
        return ResponseEntity.ok("비밀번호가 성공적으로 재설정되었습니다. 새로운 비밀번호로 로그인해주세요.");
    }

    /** 8. 세션 로그아웃 API */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // 세션 무효화
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of(
                "message", "성공적으로 로그아웃되었습니다.",
                "redirectUrl", "/login"
        ));
    }

    /** 9. 현재 로그인된 세션 사용자 정보 조회 API */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인되지 않은 사용자입니다."));
        }
        return ResponseEntity.ok(Map.of(
                "principal", authentication.getName(),
                "authorities", authentication.getAuthorities()
        ));
    }



    /** 10. CSRF 토큰 확인용 API */
    @GetMapping("/csrf")
    public ResponseEntity<CsrfToken> getCsrfToken(CsrfToken csrfToken) {
        return ResponseEntity.ok(csrfToken);
    }
}

