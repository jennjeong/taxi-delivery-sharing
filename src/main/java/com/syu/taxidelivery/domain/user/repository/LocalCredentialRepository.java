package com.syu.taxidelivery.domain.user.repository;

import com.syu.taxidelivery.domain.user.entity.LocalCredential;
import com.syu.taxidelivery.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * [LocalCredential Repository 인터페이스]
 *
 * Spring Data JPA를 활용하여 LOCAL_CREDENTIALS 테이블(자체 로그인 비밀번호, 잠금 정보 등)에
 * 접근하는 데이터 레이어(DAO) 인터페이스입니다.
 */
public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {

    /**
     * 1. 유저 ID(Primary Key) 값으로 자체 로그인 정보 조회
     * @param userId 조회하고자 하는 User 엔티티의 식별자 ID
     * @return NullPointerException 방지를 위한 Optional 감싸기 객체
     */
    Optional<LocalCredential> findByUserId(Long userId);

    /**
     * 2. User 객체 자체를 매핑하여 자체 로그인 정보 조회 (LocalAuthService 연동용 추가)
     * @param user 조회하고자 하는 User 엔티티 객체
     * @return 해당 유저와 1:1로 매핑된 LocalCredential 객체
     */
    Optional<LocalCredential> findByUser(User user);
}