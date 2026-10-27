package com.syu.taxidelivery.domain.user.entity; // 엔티티가 속한 패키지 경로

import com.syu.taxidelivery.domain.user.enums.Role; // 사용자 권한 Enum (STUDENT, ADMIN)
import com.syu.taxidelivery.domain.user.enums.UserStatus; // 사용자 상태 Enum (ACTIVE, INACTIVE, BLOCKED)
import jakarta.persistence.*; // JPA 표준 어노테이션 패키지 (@Entity, @Table, @Id 등)
import lombok.*; // Lombok 라이브러리 패키지 (@Getter, @Builder 등)
import org.hibernate.annotations.CreationTimestamp; // 최초 저장 시 시간을 자동 입력하는 하이버네이트 어노테이션
import org.hibernate.annotations.UpdateTimestamp; // 수정 발생 시 시간을 자동 입력하는 하이버네이트 어노테이션

import java.time.LocalDateTime; // 날짜와 시간 정보를 다루는 Java 8+ 기본 클래스

@Entity // JPA가 이 클래스를 DB의 USERS 테이블과 매핑하도록 지정
@Table(name = "USERS") // 데이터베이스의 'USERS' 테이블과 명시적 매핑
@Getter // 모든 필드에 대한 getter 메서드를 자동으로 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자를 작성하되, 외부 직접 인스턴스 생성을 막기 위해 PROTECTED로 설정 (JPA 스펙 요구사항)
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자 자동 생성 (@Builder 동작 시 필요)
@Builder // 빌더 패턴을 적용하여 객체를 안전하고 가독성 높게 생성
public class User {

    @Id // PK(Primary Key, 기본키) 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB의 AUTO_INCREMENT 기능을 사용하여 PK 값 자동 생성
    private Long id; // 사용자 고유 식별자 ID (PK)

    @Column(name = "student_no", nullable = false, unique = true, length = 20) // 필수값, 유니크 제약조건, 최대 20자
    private String studentNo; // 학번 (UNIQUE, NOT NULL)

    @Column(nullable = false, length = 50) // 필수값, 최대 50자
    private String name; // 사용자 이름 (NOT NULL)

    @Column(name = "school_email", nullable = false, unique = true, length = 100) // 필수값, 유니크 제약조건, 최대 100자
    private String schoolEmail; // 학교 이메일 주소 (UNIQUE, NOT NULL)

    @Enumerated(EnumType.STRING) // Enum 이름을 DB에 문자열 그대로 저장 ("STUDENT", "ADMIN")
    @Column(nullable = false, length = 20) // 필수값, 최대 20자
    @Builder.Default // 빌더 사용 시 기본값(STUDENT) 적용
    private Role role = Role.STUDENT; // 사용자 권한 (기본값: STUDENT)

    @Enumerated(EnumType.STRING) // Enum 이름을 DB에 문자열 그대로 저장 ("ACTIVE", "INACTIVE" 등)
    @Column(nullable = false, length = 20) // 필수값, 최대 20자
    @Builder.Default // 빌더 사용 시 기본값(ACTIVE) 적용
    private UserStatus status = UserStatus.ACTIVE; // 계정 상태 (기본값: ACTIVE)

    @Column(name = "last_login_at") // 최근 로그인 일시 (NULL 허용)
    private LocalDateTime lastLoginAt; // 마지막 로그인 시각

    @CreationTimestamp // 엔티티가 최초 저장(INSERT)되는 시점의 현재 시간을 자동으로 기록
    @Column(name = "created_at", nullable = false, updatable = false) // 필수값, 이후 수정 불가(updatable = false)
    private LocalDateTime createdAt; // 계정 생성 일시

    @UpdateTimestamp // 엔티티가 수정(UPDATE)되는 시점의 현재 시간을 자동으로 갱신
    @Column(name = "updated_at", nullable = false) // 필수값
    private LocalDateTime updatedAt; // 계정 정보 마지막 수정 일시

    /**
     * [최근 로그인 시간 갱신 메서드]
     * 로그인 성공 시 호출되어 사용자의 최근 로그인 시각을 현재 시각으로 변경합니다.
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * [사용자 이름 수정 메서드]
     * 마이페이지 등에서 사용자 이름을 변경할 때 사용합니다.
     * @param name 변경할 이름
     */
    public void updateName(String name) {
        this.name = name;
    }

    /**
     * [계정 상태 변경 메서드]
     * 차단(BLOCKED) 또는 탈퇴(INACTIVE) 처리 시 사용합니다.
     * @param status 변경할 상태값
     */
    public void updateStatus(UserStatus status) {
        this.status = status;
    }
}