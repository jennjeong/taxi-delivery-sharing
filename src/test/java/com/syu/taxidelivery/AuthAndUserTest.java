package com.syu.taxidelivery;

import com.syu.taxidelivery.domain.user.dto.ResetPasswordRequest;
import com.syu.taxidelivery.domain.user.dto.SchoolRegisterRequest;
import com.syu.taxidelivery.domain.user.dto.SendMailCodeRequest;
import com.syu.taxidelivery.domain.user.dto.VerifyMailCodeRequest;
import com.syu.taxidelivery.domain.user.entity.LocalCredential;
import com.syu.taxidelivery.domain.user.entity.User;
import com.syu.taxidelivery.domain.user.enums.Role;
import com.syu.taxidelivery.domain.user.enums.UserStatus;
import com.syu.taxidelivery.domain.user.repository.LocalCredentialRepository;
import com.syu.taxidelivery.domain.user.repository.UserAuthRepository;
import com.syu.taxidelivery.domain.user.repository.UserRepository;
import com.syu.taxidelivery.domain.user.service.SchoolEmailAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthAndUserTest {

    @InjectMocks
    private SchoolEmailAuthService schoolEmailAuthService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocalCredentialRepository localCredentialRepository;

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private PasswordEncoder passwordEncoder;

    /**
     * [테스트 1] 틀린 인증번호 입력 시 예외 발생 검증 (TC-AUTH-01 & 02)
     */
    @Test
    @DisplayName("TC-AUTH-01 & 02: 이메일 인증 번호 검증 실패 테스트")
    void verifySchoolEmailTest() {
        SendMailCodeRequest sendReq = new SendMailCodeRequest();
        ReflectionTestUtils.setField(sendReq, "schoolEmail", "20230001@syuin.ac.kr");

        try {
            schoolEmailAuthService.sendVerificationCode(sendReq);
        } catch (Exception ignored) {}

        VerifyMailCodeRequest wrongVerifyReq = new VerifyMailCodeRequest();
        ReflectionTestUtils.setField(wrongVerifyReq, "schoolEmail", "20230001@syuin.ac.kr");
        ReflectionTestUtils.setField(wrongVerifyReq, "code", "000000");

        assertThatThrownBy(() -> schoolEmailAuthService.verifyCode(wrongVerifyReq))
                .isInstanceOf(Throwable.class);
    }

    /**
     * [테스트 2] 허용되지 않은 이메일 도메인 예외 처리 검증 (TC-AUTH-03)
     */
    @Test
    @DisplayName("TC-AUTH-03: 허용되지 않은 이메일 도메인 입력 예외")
    void invalidEmailDomainTest() {
        SendMailCodeRequest request = new SendMailCodeRequest();
        ReflectionTestUtils.setField(request, "schoolEmail", "test@gmail.com");

        assertThatThrownBy(() -> schoolEmailAuthService.sendVerificationCode(request))
                .isInstanceOf(Throwable.class);
    }

    /**
     * [테스트 3] 이미 가입된 학교 이메일로 인증번호 발송 요청 시 차단
     */
    @Test
    @DisplayName("TC-AUTH-04: 이미 가입된 학교 이메일 중복 인증 차단 테스트")
    void duplicateEmailSendCode_ThrowsException() {
        String email = "duplicate@syuin.ac.kr";
        SendMailCodeRequest request = new SendMailCodeRequest();
        ReflectionTestUtils.setField(request, "schoolEmail", email);

        given(userRepository.existsBySchoolEmail(email)).willReturn(true);

        assertThatThrownBy(() -> schoolEmailAuthService.sendVerificationCode(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 가입된 학교 이메일");
    }

    /**
     * [테스트 4] 이메일 미인증 상태에서 회원가입 시도 시 차단 검증 (TC-USER-01 & 02)
     */
    @Test
    @DisplayName("TC-USER-01 & 02: 미인증 이메일 가입 차단 테스트")
    void registerWithUnverifiedEmailTest() {
        SchoolRegisterRequest regReq = new SchoolRegisterRequest();
        ReflectionTestUtils.setField(regReq, "studentNo", "20230001");
        ReflectionTestUtils.setField(regReq, "name", "홍길동");
        ReflectionTestUtils.setField(regReq, "schoolEmail", "20230001@syuin.ac.kr");
        ReflectionTestUtils.setField(regReq, "password", "password123!");

        assertThatThrownBy(() -> schoolEmailAuthService.registerWithSchoolEmail(regReq))
                .isInstanceOf(Throwable.class);
    }

    /**
     * [테스트 5] 중복 학번으로 가입 시도 시 차단 검증
     */
    @Test
    @DisplayName("TC-USER-DUPLICATE-01: 중복 학번 회원가입 차단 테스트")
    void registerWithDuplicateStudentNo_ThrowsException() {
        String email = "20230002@syuin.ac.kr";
        Object verifiedMap = ReflectionTestUtils.getField(schoolEmailAuthService, "verifiedEmails");
        ((Map<String, Boolean>) verifiedMap).put(email, true);

        given(userRepository.existsByStudentNo("20230002")).willReturn(true);

        SchoolRegisterRequest regReq = new SchoolRegisterRequest();
        ReflectionTestUtils.setField(regReq, "studentNo", "20230002");
        ReflectionTestUtils.setField(regReq, "name", "김철수");
        ReflectionTestUtils.setField(regReq, "schoolEmail", email);
        ReflectionTestUtils.setField(regReq, "password", "password123!");

        assertThatThrownBy(() -> schoolEmailAuthService.registerWithSchoolEmail(regReq))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 등록된 학번");
    }

    /**
     * [테스트 6] 이메일 인증 완료 후 정상 회원가입 성공 검증 (TC-USER-03)
     */
    @Test
    @DisplayName("TC-USER-03: 인증 완료 후 회원가입 성공 테스트")
    void registerWithVerifiedEmail_Success() {
        String email = "20230001@syuin.ac.kr";

        Object verifiedMap = ReflectionTestUtils.getField(schoolEmailAuthService, "verifiedEmails");
        ((Map<String, Boolean>) verifiedMap).put(email, true);

        User mockUser = User.builder()
                .studentNo("20230001")
                .name("홍길동")
                .schoolEmail(email)
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();

        given(userRepository.existsBySchoolEmail(email)).willReturn(false);
        given(userRepository.existsByStudentNo("20230001")).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(mockUser);
        given(passwordEncoder.encode("password123!")).willReturn("encodedPassword");

        SchoolRegisterRequest regReq = new SchoolRegisterRequest();
        ReflectionTestUtils.setField(regReq, "studentNo", "20230001");
        ReflectionTestUtils.setField(regReq, "name", "홍길동");
        ReflectionTestUtils.setField(regReq, "schoolEmail", email);
        ReflectionTestUtils.setField(regReq, "password", "password123!");

        User savedUser = schoolEmailAuthService.registerWithSchoolEmail(regReq);

        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getSchoolEmail()).isEqualTo(email);
    }

    /**
     * [테스트 7] 비밀번호 재설정용 코드 발송 시 미가입 이메일 차단 검증
     */
    @Test
    @DisplayName("TC-RESET-01: 미가입 이메일로 비밀번호 재설정 코드 발송 시 차단")
    void sendResetPasswordCode_UnregisteredEmail_ThrowsException() {
        String email = "notfound@syuin.ac.kr";
        SendMailCodeRequest request = new SendMailCodeRequest();
        ReflectionTestUtils.setField(request, "schoolEmail", email);

        given(userRepository.existsBySchoolEmail(email)).willReturn(false);

        assertThatThrownBy(() -> schoolEmailAuthService.sendResetPasswordCode(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("가입되지 않은 학교 이메일");
    }

    /**
     * [테스트 8] 학교 이메일 재인증을 통한 비밀번호 재설정 성공 검증
     */
    @Test
    @DisplayName("TC-RESET-02: 학교 이메일 재인증을 통한 비밀번호 재설정 성공")
    void resetPassword_Success() {
        String email = "20230001@syuin.ac.kr";

        // 1. 재인증 완료 상태 주입
        Object verifiedResetMap = ReflectionTestUtils.getField(schoolEmailAuthService, "verifiedResetEmails");
        ((Map<String, Boolean>) verifiedResetMap).put(email, true);

        // 2. Mock 유저 및 크리덴셜
        User mockUser = User.builder()
                .studentNo("20230001")
                .name("홍길동")
                .schoolEmail(email)
                .build();

        LocalCredential credential = LocalCredential.builder()
                .user(mockUser)
                .passwordHash("old_password_hash")
                .failedLoginCount(5)
                .build();

        given(userRepository.findBySchoolEmail(email)).willReturn(Optional.of(mockUser));
        given(localCredentialRepository.findByUser(mockUser)).willReturn(Optional.of(credential));
        given(passwordEncoder.encode("newPassword123!")).willReturn("new_password_hash");

        // 3. 재설정 요청 실행
        ResetPasswordRequest request = new ResetPasswordRequest(email, "newPassword123!");
        schoolEmailAuthService.resetPassword(request);

        // 4. 새 비밀번호 및 실패 횟수/잠금 초기화 검증
        assertThat(credential.getPasswordHash()).isEqualTo("new_password_hash");
        assertThat(credential.getFailedLoginCount()).isEqualTo(0);
        assertThat(credential.isLocked()).isFalse();
    }
}
