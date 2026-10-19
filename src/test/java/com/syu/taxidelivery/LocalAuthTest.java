//자체 로그인 및 계정 잠금 단위 테스트
package com.syu.taxidelivery;

import com.school.party.domain.user.dto.LocalLoginRequest;
import com.school.party.domain.user.entity.LocalCredential;
import com.school.party.domain.user.entity.User;
import com.school.party.domain.user.enums.Role;
import com.school.party.domain.user.enums.UserStatus;
import com.school.party.domain.user.repository.LocalCredentialRepository;
import com.school.party.domain.user.repository.UserRepository;
import com.school.party.domain.user.service.LocalAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LocalAuthTest {

    @InjectMocks
    private LocalAuthService localAuthService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocalCredentialRepository localCredentialRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    /**
     * [테스트 1] 정상 로그인 성공 검증
     */
    @Test
    @DisplayName("TC-AUTH-FALLBACK-01: 올바른 아이디/비밀번호 입력 시 로그인 성공")
    void login_Success() {
        // 1. 가짜 유저 및 로그인 자격 증명 생성
        User mockUser = User.builder()
                .studentNo("20230001")
                .name("홍길동")
                .schoolEmail("20230001@syuin.ac.kr")
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();

        LocalCredential credential = LocalCredential.builder()
                .user(mockUser)
                .passwordHash("encoded_password")
                .failedLoginCount(0)
                .build();

        // 2. 요청 DTO 생성 (학번 또는 이메일 식별자)
        LocalLoginRequest request = new LocalLoginRequest();
        ReflectionTestUtils.setField(request, "identifier", "20230001@syuin.ac.kr");
        ReflectionTestUtils.setField(request, "password", "password123!");

        // 3. Mockito 동작 모킹
        given(userRepository.findBySchoolEmail("20230001@syuin.ac.kr")).willReturn(Optional.of(mockUser));
        given(localCredentialRepository.findByUser(mockUser)).willReturn(Optional.of(credential));
        given(passwordEncoder.matches("password123!", "encoded_password")).willReturn(true);

        // 4. 로그인 실행
        User loggedInUser = localAuthService.login(request);

        // 5. 검증
        assertThat(loggedInUser).isNotNull();
        assertThat(loggedInUser.getSchoolEmail()).isEqualTo("20230001@syuin.ac.kr");
        assertThat(credential.getFailedLoginCount()).isEqualTo(0);
    }

    /**
     * [테스트 2] 비밀번호 틀릴 시 실패 횟수 증가 검증
     */
    @Test
    @DisplayName("TC-AUTH-FALLBACK-02: 비밀번호 불일치 시 실패 횟수 증가 및 예외 발생")
    void login_WrongPassword_IncreaseFailedCount() {
        User mockUser = User.builder()
                .studentNo("20230001")
                .schoolEmail("20230001@syuin.ac.kr")
                .build();

        LocalCredential credential = LocalCredential.builder()
                .user(mockUser)
                .passwordHash("encoded_password")
                .failedLoginCount(0)
                .build();

        LocalLoginRequest request = new LocalLoginRequest();
        ReflectionTestUtils.setField(request, "identifier", "20230001@syuin.ac.kr");
        ReflectionTestUtils.setField(request, "password", "wrong_password");

        given(userRepository.findBySchoolEmail("20230001@syuin.ac.kr")).willReturn(Optional.of(mockUser));
        given(localCredentialRepository.findByUser(mockUser)).willReturn(Optional.of(credential));
        given(passwordEncoder.matches("wrong_password", "encoded_password")).willReturn(false);

        // 예외 발생 검증 및 실패 횟수 1 증가 확인
        assertThatThrownBy(() -> localAuthService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");

        assertThat(credential.getFailedLoginCount()).isEqualTo(1);
    }

    /**
     * [테스트 3] 계정 잠금 기능 검증 (5회 이상 실패 시 30분 잠금 처리)
     */
    @Test
    @DisplayName("TC-AUTH-FALLBACK-03: 계정 잠금 상태에서 로그인 시도 시 접근 차단 예외")
    void login_LockedAccount_ThrowException() {
        User mockUser = User.builder()
                .studentNo("20230001")
                .schoolEmail("20230001@syuin.ac.kr")
                .build();

        // 30분간 계정이 잠긴 상태 생성
        LocalCredential credential = LocalCredential.builder()
                .user(mockUser)
                .passwordHash("encoded_password")
                .failedLoginCount(5)
                .build();
        credential.lockAccount(LocalDateTime.now().plusMinutes(30));

        LocalLoginRequest request = new LocalLoginRequest();
        ReflectionTestUtils.setField(request, "identifier", "20230001@syuin.ac.kr");
        ReflectionTestUtils.setField(request, "password", "password123!");

        given(userRepository.findBySchoolEmail("20230001@syuin.ac.kr")).willReturn(Optional.of(mockUser));
        given(localCredentialRepository.findByUser(mockUser)).willReturn(Optional.of(credential));

        // 잠긴 계정 로그인 시도 시 IllegalStateException 차단 검증
        assertThatThrownBy(() -> localAuthService.login(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("계정이 잠겼습니다");
    }
}