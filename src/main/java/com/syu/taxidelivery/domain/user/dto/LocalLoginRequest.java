package com.syu.taxidelivery.domain.user.dto; // DTO가 속한 패키지 경로

import jakarta.validation.constraints.NotBlank; // 필수 입력값 유효성 검증 어노테이션
import lombok.Getter; // getter 메서드 자동 생성 Lombok 어노테이션
import lombok.NoArgsConstructor; // 파라미터가 없는 기본 생성자 자동 생성 Lombok 어노테이션

/**
 * [자체 로그인 요청 DTO]
 * 클라이언트(프론트엔드)에서 자체 로그인(Fallback) 요청 시 전달하는 데이터 전송 객체입니다.
 */
@Getter // JSON 요청 데이터를 객체 필드로 매핑하거나 조회할 때 사용
@NoArgsConstructor // Jackson 라이브러리가 역직렬화(JSON -> Object)할 때 필요한 기본 생성자 생성
public class LocalLoginRequest {

    /**
     * 사용자 로그인 식별자 (학번 또는 학교 이메일)
     * 예: "20230001" 또는 "20230001@syuin.ac.kr"
     */
    @NotBlank(message = "아이디(학번 또는 학교 이메일)는 필수 입력값입니다.") // null, "", " " 허용 불가
    private String identifier; // 로그인 아이디 식별자 문자열

    /**
     * 사용자 비밀번호
     */
    @NotBlank(message = "비밀번호는 필수 입력값입니다.") // null, "", " " 허용 불가
    private String password; // 암호화 검증을 위한 평문 비밀번호 문자열
}