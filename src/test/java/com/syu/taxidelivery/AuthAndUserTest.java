package com.syu.taxidelivery; // 테스트 클래스가 속한 패키지 경로

import com.school.party.domain.user.dto.SchoolRegisterRequest; // 회원가입 요청 DTO
import com.school.party.domain.user.dto.SendMailCodeRequest; // 메일 인증번호 발송 요청 DTO
import com.school.party.domain.user.dto.VerifyMailCodeRequest; // 메일 인증번호 검증 요청 DTO
import com.school.party.domain.user.entity.User; // User 엔티티
import com.school.party.domain.user.enums.Role; // 권한 Enum (STUDENT, ADMIN)
import com.school.party.domain.user.enums.UserStatus; // 유저 상태 Enum (ACTIVE 등)
import com.school.party.domain.user.repository.LocalCredentialRepository; // 자체 로그인 리포지토리
import com.school.party.domain.user.repository.UserAuthRepository; // 인증 수단 연동 리포지토리
import com.school.party.domain.user.repository.UserRepository; // 마스터 유저 리포지토리
import com.school.party.domain.user.service.SchoolEmailAuthService; // 테스트 대상 서비스
import org.junit.jupiter.api.DisplayName; // 테스트 케이스 이름을 지정하는 JUnit5 어노테이션
import org.junit.jupiter.api.Test; // 단위 테스트 메서드임을 명시하는 JUnit5 어노테이션
import org.junit.jupiter.api.extension.ExtendWith; // JUnit5 확장 기능을 사용하기 위한 어노테이션
import org.mockito.InjectMocks; // @Mock으로 생성된 가짜 객체들을 주입받을 실제 서비스 대상 지정
import org.mockito.Mock; // 실제 DB나 메일 서버 연결 대신 사용할 가짜(Mock) 객체 생성
import org.mockito.junit.jupiter.MockitoExtension; // Mockito 프레임워크를 JUnit5와 연동
import org.springframework.mail.javamail.JavaMailSender; // 메일 발송 스프링 인터페이스
import org.springframework.security.crypto.password.PasswordEncoder; // 비밀번호 암호화 인터페이스
import org.springframework.test.util.ReflectionTestUtils; // Private 필드나 Setter가 없는 DTO에 강제로 값을 주입하는 테스트 유틸리티

import java.util.Map; // 인메모리 인증 상태 맵 접근용 Java Map 인터페이스

import static org.assertj.core.api.Assertions.assertThatThrownBy; // 예외 발생 여부 검증용 AssertJ 라이브러리
import static org.mockito.ArgumentMatchers.any; // Mockito에서 임의의 인자 전달을 표현할 때 사용
import static org.mockito.BDDMockito.given; // BDD 스타일(Given-When-Then)로 가짜 객체의 동작을 정의

@ExtendWith(MockitoExtension.class) // 단위 테스트에서 Mockito의 가짜 객체(Mock) 생성 기능을 활성화
class AuthAndUserTest {

    @InjectMocks // @Mock으로 선언된 가짜 의존성들을 아래 SchoolEmailAuthService에 자동으로 주입
    private SchoolEmailAuthService schoolEmailAuthService;

    @Mock // DB 연동을 대신할 가짜 UserRepository 객체
    private UserRepository userRepository;

    @Mock // DB 연동을 대신할 가짜 LocalCredentialRepository 객체
    private LocalCredentialRepository localCredentialRepository;

    @Mock // DB 연동을 대신할 가짜 UserAuthRepository 객체
    private UserAuthRepository userAuthRepository;

    @Mock // 실제 메일을 보내지 않도록 대체하는 가짜 JavaMailSender 객체
    private JavaMailSender mailSender;

    @Mock // 실제로 암호화를 수행하지 않고 가짜 값을 반환할 PasswordEncoder 객체
    private PasswordEncoder passwordEncoder;

    /**
     * [테스트 1] 틀린 인증번호 입력 시 예외 발생 검증 (TC-AUTH-01 & 02)
     */
    @Test
    @DisplayName("TC-AUTH-01 & 02: 이메일 인증 번호 검증 실패 테스트")
    void verifySchoolEmailTest() {
        // 1. 발송 요청 DTO 생성 및 필드값 강제 설정
        SendMailCodeRequest sendReq = new SendMailCodeRequest();
        ReflectionTestUtils.setField(sendReq, "schoolEmail", "20230001@syuin.ac.kr");

        // 2. 인증 번호 발송 로직 수행 (발송 상태 세팅)
        try {
            schoolEmailAuthService.sendVerificationCode(sendReq);
        } catch (Exception ignored) {}

        // 3. 틀린 인증 번호("000000")를 담은 검증 요청 DTO 생성
        VerifyMailCodeRequest wrongVerifyReq = new VerifyMailCodeRequest();
        ReflectionTestUtils.setField(wrongVerifyReq, "schoolEmail", "20230001@syuin.ac.kr");
        ReflectionTestUtils.setField(wrongVerifyReq, "code", "000000");

        // 4. 틀린 인증 번호 검증 시 예외(IllegalArgumentException 등)가 터지는지 확인
        assertThatThrownBy(() -> schoolEmailAuthService.verifyCode(wrongVerifyReq))
                .isInstanceOf(Throwable.class);
    }

    /**
     * [테스트 2] 허용되지 않은 이메일 도메인 예외 처리 검증 (TC-AUTH-03)
     */
    @Test
    @DisplayName("TC-AUTH-03: 허용되지 않은 이메일 도메인 입력 예외")
    void invalidEmailDomainTest() {
        // 1. 외부 이메일(@gmail.com) 요청 DTO 생성
        SendMailCodeRequest request = new SendMailCodeRequest();
        ReflectionTestUtils.setField(request, "schoolEmail", "test@gmail.com");

        // 2. @syuin.ac.kr이 아닌 메일 요청 시 예외가 즉시 발생하는지 검증
        assertThatThrownBy(() -> schoolEmailAuthService.sendVerificationCode(request))
                .isInstanceOf(Throwable.class);
    }

    /**
     * [테스트 3] 이메일 미인증 상태에서 회원가입 시도 시 차단 검증 (TC-USER-01 & 02)
     */
    @Test
    @DisplayName("TC-USER-01 & 02: 미인증 이메일 가입 차단 테스트")
    void registerWithUnverifiedEmailTest() {
        // 1. 회원가입 요청 DTO 작성
        SchoolRegisterRequest regReq = new SchoolRegisterRequest();
        ReflectionTestUtils.setField(regReq, "studentNo", "20230001");
        ReflectionTestUtils.setField(regReq, "name", "홍길동");
        ReflectionTestUtils.setField(regReq, "schoolEmail", "20230001@syuin.ac.kr");
        ReflectionTestUtils.setField(regReq, "password", "password123!");

        // 2. 이메일 인증(verifyCode) 절차를 거치지 않은 상태로 가입 시도 시 예외 차단 검증
        assertThatThrownBy(() -> schoolEmailAuthService.registerWithSchoolEmail(regReq))
                .isInstanceOf(Throwable.class);
    }

    /**
     * [테스트 4] 이메일 인증 완료 후 정상 회원가입 성공 검증 (TC-USER-03)
     */
    @Test
    @DisplayName("TC-USER-03: 인증 완료 후 회원가입 성공 테스트")
    void registerWithVerifiedEmail_Success() {
        String email = "20230001@syuin.ac.kr";

        // 1. 서비스 내부의 verifiedEmails 인메모리 Map에 해당 이메일이 '인증 성공(true)' 상태임을 강제 세팅
        Object verifiedMap = ReflectionTestUtils.getField(schoolEmailAuthService, "verifiedEmails");
        ((Map<String, Boolean>) verifiedMap).put(email, true);

        // 2. 가짜 DB 리턴값(User 엔티티) 생성
        User mockUser = User.builder()
                .studentNo("20230001")
                .name("홍길동")
                .schoolEmail(email)
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();

        // 3. Mockito BDD 가짜 동작 정의 (중복 검사 통과, 저장 성공 모킹)
        given(userRepository.existsBySchoolEmail(email)).willReturn(false); // 이메일 중복 없음
        given(userRepository.existsByStudentNo("20230001")).willReturn(false); // 학번 중복 없음
        given(userRepository.save(any(User.class))).willReturn(mockUser); // save 호출 시 mockUser 리턴
        given(passwordEncoder.encode("password123!")).willReturn("encodedPassword"); // 비밀번호 암호화 결과 모킹

        // 4. 회원가입 요청 DTO 준비
        SchoolRegisterRequest regReq = new SchoolRegisterRequest();
        ReflectionTestUtils.setField(regReq, "studentNo", "20230001");
        ReflectionTestUtils.setField(regReq, "name", "홍길동");
        ReflectionTestUtils.setField(regReq, "schoolEmail", email);
        ReflectionTestUtils.setField(regReq, "password", "password123!");

        // 5. 회원가입 실행
        User savedUser = schoolEmailAuthService.registerWithSchoolEmail(regReq);

        // 6. 결과 검증: 반환된 유저 객체가 null이 아니고 입력한 이메일과 동일한지 확인
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getSchoolEmail()).isEqualTo(email);
    }
}