package com.syu.taxidelivery.domain.user.entity; // 엔티티가 속한 패키지 경로

import jakarta.persistence.*; // JPA 아노테이션 패키지 (@Entity, @Table, @Id 등)
import lombok.*; // Lombok 라이브러리 패키지 (@Getter, @Builder 등)
import org.hibernate.annotations.CreationTimestamp; // 생성 시점의 시간을 자동으로 기록하는 하이버네이트 어노테이션

import java.time.LocalDateTime; // 날짜와 시간 정보를 다루는 Java 8+ 기본 클래스

@Entity // JPA가 이 클래스를 자바 객체와 데이터베이스 테이블 간 매핑 대상으로 지정
@Table(name = "LOCAL_CREDENTIAL") // 데이터베이스의 'LOCAL_CREDENTIAL' 테이블과 매핑
@Getter // 모든 필드에 대한 getter 메서드를 자동으로 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자를 생성하되, 외부에서 함부로 객체 생성을 못 하도록 PROTECTED로 제약 (JPA 스펙 권장)
@AllArgsConstructor // 모든 필드를 파라미터로 받는 생성자를 생성 (@Builder 사용 시 필요)
@Builder // 빌더 패턴을 적용하여 객체를 체이닝 형태로 쉽게 생성할 수 있도록 지원
public class LocalCredential {

    @Id // PK(Primary Key, 기본키)로 지정
    @Column(name = "user_id") // 데이터베이스 테이블의 'user_id' 컬럼과 매핑
    private Long userId; // USERS 테이블의 id와 공유하는 1:1 관계의 식별자 PK

    @MapsId // @OneToOne 관계에서 외래키(FK)를 이 엔티티의 기본키(PK)로 공유 및 사용하게 함
    @OneToOne(fetch = FetchType.LAZY) // User 엔티티와 1:1 관계 지정 (성능 최적화를 위해 지연 로딩 LAZY 설정)
    @JoinColumn(name = "user_id") // DB 상에서 참조할 외래키 컬럼 이름 지정
    private User user; // 연관된 마스터 사용자 엔티티 참조 객체

    @Column(name = "password_hash", nullable = false) // 비밀번호 해시값 저장 컬럼 (필수값 NOT NULL)
    private String passwordHash; // BCrypt 알고리즘으로 암호화된 비밀번호 문자열

    @Builder.Default // 빌더 패턴 사용 시 기본 초기값을 0으로 고정하도록 설정
    @Column(name = "failed_login_count", nullable = false) // 비밀번호 오입력 실패 횟수 컬럼 (필수값 NOT NULL)
    private int failedLoginCount = 0; // 로그인 실패 횟수 (기본값 0)

    @Column(name = "locked_until") // 계정 잠금 해제 시간 컬럼 (NULL 허용, 잠기지 않은 경우 null)
    private LocalDateTime lockedUntil; // 계정이 잠겨 있는 유효 기한 시간 정보

    @CreationTimestamp // 엔티티가 최초 저장(INSERT)될 때의 시간을 자동으로 입력
    @Column(name = "password_updated_at", nullable = false) // 비밀번호 변경/생성 일시 컬럼 (필수값 NOT NULL)
    private LocalDateTime passwordUpdatedAt; // 마지막으로 비밀번호가 변경되거나 생성된 일시

    /**
     * [비밀번호 변경 및 재설정 메서드]
     * 학교 이메일 재인증을 거친 후 새 비밀번호로 변경하며, 실패 횟수 및 잠금을 초기화합니다.
     */
    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordUpdatedAt = LocalDateTime.now();
        this.resetFailedLoginCount();
    }

    /**
     * [비밀번호 오입력 시 실패 횟수 증가 및 자동 잠금 처리]
     * 로그인 실패 시 횟수를 1 증가시키며, 5회 이상 연속 실패 시 계정을 30분간 자동 잠금합니다.
     */
    public void increaseFailedLoginCount() {
        this.failedLoginCount += 1; // 실패 횟수 1 증가
        if (this.failedLoginCount >= 5) { // 5회 이상 실패 시
            this.lockedUntil = LocalDateTime.now().plusMinutes(30); // 현재 시간 기준으로 30분간 계정 잠금 설정
        }
    }

    /**
     * [계정 임의 잠금 메서드]
     * 수동으로 특정 시점까지 계정을 잠그고자 할 때 사용합니다.
     */
    public void lockAccount(LocalDateTime lockTime) {
        this.lockedUntil = lockTime; // 지정된 잠금 만료 시간 설정
    }

    /**
     * [로그인 성공 시 실패 횟수 초기화 및 잠금 해제]
     * 올바른 비밀번호로 로그인에 성공하면 기존 실패 기록과 잠금 시간을 초기화합니다.
     */
    public void resetFailedLoginCount() {
        this.failedLoginCount = 0; // 실패 횟수를 0으로 리셋
        this.lockedUntil = null; // 잠금 상태 해제 (null)
    }

    /**
     * [계정 잠금 여부 확인 메서드]
     * 현재 시점 기준으로 계정이 잠겨 있는지 여부를 확인합니다.
     * @return 잠금 시간이 존재하고, 아직 현재 시간보다 미래이면 true 반환
     */
    public boolean isLocked() {
        return this.lockedUntil != null && this.lockedUntil.isAfter(LocalDateTime.now());
    }
}