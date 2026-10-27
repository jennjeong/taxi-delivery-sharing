package com.syu.taxidelivery.domain.user.entity; // 엔티티가 속한 패키지 경로

import com.syu.taxidelivery.domain.user.enums.AuthProvider; // 인증 제공자 구분 Enum (SCHOOL_SSO, LOCAL 등)
import jakarta.persistence.*; // JPA 아노테이션 패키지 (@Entity, @Table, @Id 등)
import lombok.*; // Lombok 라이브러리 패키지 (@Getter, @Builder 등)
import org.hibernate.annotations.CreationTimestamp; // 생성 시점의 시간을 자동으로 기록하는 하이버네이트 어노테이션

import java.time.LocalDateTime; // 날짜와 시간 정보를 다루는 Java 8+ 기본 클래스

@Entity // JPA가 이 클래스를 DB 테이블 매핑 대상으로 지정
@Table(
        name = "USER_AUTH", // 데이터베이스의 'USER_AUTH' 테이블과 매핑
        uniqueConstraints = { // DB 복합 유니크 제약조건 설정
                @UniqueConstraint(columnNames = {"provider", "provider_user_id"}) // 제공자(provider)와 식별자(provider_user_id) 조합은 중복 불가
        }
)
@Getter // 모든 필드에 대한 getter 메서드를 자동으로 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자를 생성하되, 외부 직접 호출 방지를 위해 PROTECTED 제약 (JPA 스펙)
@AllArgsConstructor // 모든 필드를 파라미터로 받는 생성자 생성 (@Builder 사용에 필요)
@Builder // 빌더 패턴을 적용하여 객체 생성을 안전하고 직관적으로 지원
public class UserAuth {

    @Id // PK(Primary Key, 기본키) 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 데이터베이스 AUTO_INCREMENT 방식으로 PK 값 자동 생성
    private Long id; // USER_AUTH 테이블의 식별자 고유 ID (PK)

    @OneToOne(fetch = FetchType.LAZY) // User 엔티티와 1:1 매핑 (성능 최적화를 위해 지연 로딩 LAZY 설정)
    @JoinColumn(name = "user_id", nullable = false, unique = true) // USERS 테이블의 id를 참조하는 외래키(FK) 지정
    private User user; // 연관된 마스터 사용자(User) 엔티티 객체

    @Enumerated(EnumType.STRING) // Enum 이름을 DB에 문자열 그대로 저장 (예: "SCHOOL_SSO", "LOCAL")
    @Column(nullable = false, length = 20) // 필수값 NOT NULL 설정 및 최대 길이 20자 지정
    private AuthProvider provider; // 인증 수단 제공자 구분 Enum

    @Column(name = "provider_user_id", nullable = false) // 인증 제공자 측의 사용자 식별 ID (학교 이메일 등)
    private String providerUserId; // 외부 인증 또는 이메일 식별자 값 (NOT NULL)

    @CreationTimestamp // 엔티티가 최초 DB에 INSERT될 때 현재 시간을 자동으로 기록
    @Column(name = "linked_at", nullable = false, updatable = false) // 최초 연동 일시 (수정 불가)
    private LocalDateTime linkedAt; // 계정 연동 및 생성 시각

    @Column(name = "last_used_at") // 해당 인증 수단을 사용하여 마지막으로 로그인/인증한 시각 (NULL 허용)
    private LocalDateTime lastUsedAt; // 최근 사용 일시

    /**
     * [최근 사용 시각 업데이트 메서드]
     * 해당 인증 수단으로 로그인 성공 시 최근 사용 일시(lastUsedAt)를 현재 시간으로 갱신합니다.
     */
    public void updateLastUsedAt() {
        this.lastUsedAt = LocalDateTime.now();
    }
}