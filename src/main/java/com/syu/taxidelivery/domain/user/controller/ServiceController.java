package com.syu.taxidelivery.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * [인증된 서비스 화면 및 권한 검증용 컨트롤러]
 * - /api/service/main : 로그인 성공 후 이동하는 기본 인증 서비스 화면
 * - /api/student/** : 학생(STUDENT) 및 관리자(ADMIN) 접근 가능
 * - /api/admin/** : 관리자(ADMIN) 전용
 */
@RestController
@RequestMapping("/api")
public class ServiceController {

    /** 로그인 성공 후 이동하는 메인 서비스 화면 엔드포인트 */
    @GetMapping("/service/main")
    public ResponseEntity<Map<String, Object>> mainService(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "택시·배달 합승 메인 서비스 화면에 오신 것을 환영합니다.",
                "user", authentication.getName(),
                "authorities", authentication.getAuthorities()
        ));
    }

    /** 학생 권한 서비스 엔드포인트 */
    @GetMapping("/student/party")
    public ResponseEntity<Map<String, String>> studentPartyService() {
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "학생 및 관리자 전용 합승 파티 목록 화면입니다."
        ));
    }

    /** 관리자 전용 대시보드 엔드포인트 */
    @GetMapping("/admin/dashboard")
    public ResponseEntity<Map<String, String>> adminDashboard() {
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "관리자 전용 대시보드 화면입니다."
        ));
    }
}
