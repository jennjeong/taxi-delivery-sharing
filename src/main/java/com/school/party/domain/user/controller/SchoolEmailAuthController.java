package com.school.party.domain.user.controller;

import com.school.party.domain.user.dto.LocalLoginRequest;
import com.school.party.domain.user.dto.SchoolRegisterRequest;
import com.school.party.domain.user.dto.SendMailCodeRequest;
import com.school.party.domain.user.dto.VerifyMailCodeRequest;
import com.school.party.domain.user.entity.User;
import com.school.party.domain.user.service.LocalAuthService;
import com.school.party.domain.user.service.SchoolEmailAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/school-email")
@RequiredArgsConstructor
public class SchoolEmailAuthController {

    private final SchoolEmailAuthService schoolEmailAuthService;
    private final LocalAuthService localAuthService;

    /** 인증코드 발송 API */
    @PostMapping("/send-code")
    public ResponseEntity<String> sendCode(@Valid @RequestBody SendMailCodeRequest request) {
        schoolEmailAuthService.sendVerificationCode(request);
        return ResponseEntity.ok("학교 이메일로 인증 코드가 발송되었습니다.");
    }

    /** 인증코드 검증 API */
    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@Valid @RequestBody VerifyMailCodeRequest request) {
        schoolEmailAuthService.verifyCode(request);
        return ResponseEntity.ok("학교 이메일 인증이 성공적으로 완료되었습니다.");
    }

    /** 최종 회원가입 완료 API */
    @PostMapping("/register")
    public ResponseEntity<Long> register(@Valid @RequestBody SchoolRegisterRequest request) {
        User registeredUser = schoolEmailAuthService.registerWithSchoolEmail(request);
        return ResponseEntity.ok(registeredUser.getId());
    }

    /** 자체 로그인 (Fallback) API */
    @PostMapping("/login")
    public ResponseEntity<Long> login(@Valid @RequestBody LocalLoginRequest request) {
        User user = localAuthService.login(request);
        return ResponseEntity.ok(user.getId());
    }
}