package com.syu.taxidelivery.domain.user.service;

import com.syu.taxidelivery.domain.user.dto.LocalLoginRequest;
import com.syu.taxidelivery.domain.user.entity.LocalCredential;
import com.syu.taxidelivery.domain.user.entity.User;
import com.syu.taxidelivery.domain.user.repository.LocalCredentialRepository;
import com.syu.taxidelivery.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [자체 로그인 Fallback 서비스]
 * 학교 이메일 또는 학번 기반의 자체 비밀번호 인증 로그인 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalAuthService {

    private final UserRepository userRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * [자체 로그인 처리 메서드]
     * 학번 또는 이메일 식별자(identifier)와 비밀번호를 검증하여 로그인 처리를 수행합니다.
     */
    @Transactional
    public User login(LocalLoginRequest request) {
        String identifier = request.getIdentifier();

        // 1. 학교 이메일 또는 학번으로 사용자 조회
        User user = userRepository.findBySchoolEmail(identifier)
                .orElseGet(() -> userRepository.findByStudentNo(identifier)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다.")));

        // 2. 자체 로그인 비밀번호/잠금 정보 조회
        LocalCredential credential = localCredentialRepository.findByUser(user)
                .orElseGet(() -> localCredentialRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new IllegalArgumentException("자체 로그인 정보가 존재하지 않습니다.")));

        // 3. 계정 잠금 여부 검증
        if (credential.isLocked()) {
            throw new IllegalStateException("비밀번호 5회 이상 실패로 계정이 잠겼습니다. 잠시 후 다시 시도하세요.");
        }

        // 4. 비밀번호 일치 여부 검증
        if (!passwordEncoder.matches(request.getPassword(), credential.getPasswordHash())) {
            credential.increaseFailedLoginCount(); // 실패 횟수 증가 (5회 시 자동 잠금)
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 5. 로그인 성공 처리 (실패 횟수 리셋 및 최근 로그인 시각 갱신)
        credential.resetFailedLoginCount();
        user.updateLastLogin();

        return user;
    }
}