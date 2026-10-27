/**이메일 난수 코드 생성 > 코드 검증 > user, user_auth, local_credential에 동시 저장 및 비밀번호 재설정*/
package com.syu.taxidelivery.domain.user.service;

import com.syu.taxidelivery.domain.user.dto.ResetPasswordRequest;
import com.syu.taxidelivery.domain.user.dto.SchoolRegisterRequest;
import com.syu.taxidelivery.domain.user.dto.SendMailCodeRequest;
import com.syu.taxidelivery.domain.user.dto.VerifyMailCodeRequest;
import com.syu.taxidelivery.domain.user.entity.LocalCredential;
import com.syu.taxidelivery.domain.user.entity.User;
import com.syu.taxidelivery.domain.user.entity.UserAuth;
import com.syu.taxidelivery.domain.user.enums.AuthProvider;
import com.syu.taxidelivery.domain.user.enums.Role;
import com.syu.taxidelivery.domain.user.enums.UserStatus;
import com.syu.taxidelivery.domain.user.repository.LocalCredentialRepository;
import com.syu.taxidelivery.domain.user.repository.UserAuthRepository;
import com.syu.taxidelivery.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchoolEmailAuthService {

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    // 허용할 학교 이메일 도메인 (@syuin.ac.kr로 업데이트, 대학마다 다르게 변경 가능)
    private static final String ALLOWED_EMAIL_DOMAIN = "@syuin.ac.kr";

    // [회원가입용] 인메모리 인증코드 및 인증 완료 상태 관리
    private final Map<String, String> verificationCodes = new ConcurrentHashMap<>();
    private final Map<String, Boolean> verifiedEmails = new ConcurrentHashMap<>();

    // [비밀번호 재설정용] 인메모리 인증코드 및 인증 완료 상태 관리
    private final Map<String, String> resetPasswordCodes = new ConcurrentHashMap<>();
    private final Map<String, Boolean> verifiedResetEmails = new ConcurrentHashMap<>();

    /**
     * [1단계: 학교 이메일로 6자리 회원가입 인증코드 발송]
     * ERD 제약조건: school_email UNIQUE 및 학교 도메인 검증
     */
    public void sendVerificationCode(SendMailCodeRequest request) {
        String email = request.getSchoolEmail();

        // 1. 학교 도메인 이메일 여부 검증
        if (!email.endsWith(ALLOWED_EMAIL_DOMAIN)) {
            throw new IllegalArgumentException("학교 공식 이메일(" + ALLOWED_EMAIL_DOMAIN + ")만 사용할 수 있습니다.");
        }

        // 2. 이미 가입된 학교 이메일인지 중복 검증
        if (userRepository.existsBySchoolEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 학교 이메일입니다.");
        }

        // 3. 6자리 난수 인증코드 생성
        String code = String.format("%06d", new Random().nextInt(1000000));
        verificationCodes.put(email, code);

        // 4. 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[택시·배달 합승] 학교 학생 인증 번호 안내");
        message.setText("안녕하세요. 학생 인증을 위한 6자리 인증 번호는 [" + code + "] 입니다.");
        mailSender.send(message);
    }

    /**
     * [2단계: 사용자가 입력한 회원가입 인증코드 검증]
     */
    public void verifyCode(VerifyMailCodeRequest request) {
        String email = request.getSchoolEmail();
        String inputCode = request.getCode();

        String savedCode = verificationCodes.get(email);
        if (savedCode == null) {
            throw new IllegalArgumentException("인증 코드가 만료되었거나 발송 요청 내역이 존재하지 않습니다.");
        }
        if (!savedCode.equals(inputCode)) {
            throw new IllegalArgumentException("인증 번호가 일치하지 않습니다.");
        }

        // 인증 성공 처리 및 임시 코드 제거
        verificationCodes.remove(email);
        verifiedEmails.put(email, true);
    }

    /**
     * [3단계: 이메일 인증 성공 후 회원가입 및 ERD 테이블 자동 생성]
     * USERS ➔ USER_AUTH ➔ LOCAL_CREDENTIAL 3개 테이블 연동 생성
     */
    @Transactional
    public User registerWithSchoolEmail(SchoolRegisterRequest request) {
        String email = request.getSchoolEmail();

        // 1. 이메일 인증 수행 여부 검증
        if (!Boolean.TRUE.equals(verifiedEmails.get(email))) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

        // 2. 학번 중복 검증 (ERD UNIQUE 제약조건)
        if (userRepository.existsByStudentNo(request.getStudentNo())) {
            throw new IllegalArgumentException("이미 등록된 학번입니다.");
        }
        if (userRepository.existsBySchoolEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 학교 이메일입니다.");
        }

        // 3. USERS 마스터 엔티티 생성 및 저장
        User user = User.builder()
                .studentNo(request.getStudentNo())
                .name(request.getName())
                .schoolEmail(email)
                .role(Role.STUDENT)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        // 4. USER_AUTH 테이블 생성
        UserAuth userAuth = UserAuth.builder()
                .user(user)
                .provider(AuthProvider.SCHOOL_SSO)
                .providerUserId(email)
                .build();
        userAuthRepository.save(userAuth);

        // 5. LOCAL_CREDENTIAL 테이블 생성
        LocalCredential credential = LocalCredential.builder()
                .user(user)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .failedLoginCount(0)
                .build();
        localCredentialRepository.save(credential);

        // 사용한 인증 정보 제거
        verifiedEmails.remove(email);

        return user;
    }

    /**
     * [비밀번호 재설정 1단계: 학교 이메일 재인증 코드 발송]
     * 가입된 이메일인지 검증 후 6자리 인증코드 발송
     */
    public void sendResetPasswordCode(SendMailCodeRequest request) {
        String email = request.getSchoolEmail();

        // 1. 학교 도메인 검증
        if (!email.endsWith(ALLOWED_EMAIL_DOMAIN)) {
            throw new IllegalArgumentException("학교 공식 이메일(" + ALLOWED_EMAIL_DOMAIN + ")만 사용할 수 있습니다.");
        }

        // 2. 가입된 회원인지 확인 (미가입 계정은 비밀번호 재설정 불가)
        if (!userRepository.existsBySchoolEmail(email)) {
            throw new IllegalArgumentException("가입되지 않은 학교 이메일입니다.");
        }

        // 3. 6자리 난수 인증코드 생성
        String code = String.format("%06d", new Random().nextInt(1000000));
        resetPasswordCodes.put(email, code);

        // 4. 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[택시·배달 합승] 비밀번호 재설정 인증 번호 안내");
        message.setText("비밀번호 재설정을 위한 6자리 인증 번호는 [" + code + "] 입니다.");
        mailSender.send(message);
    }

    /**
     * [비밀번호 재설정 2단계: 인증코드 검증]
     */
    public void verifyResetPasswordCode(VerifyMailCodeRequest request) {
        String email = request.getSchoolEmail();
        String inputCode = request.getCode();

        String savedCode = resetPasswordCodes.get(email);
        if (savedCode == null) {
            throw new IllegalArgumentException("인증 코드가 만료되었거나 발송 요청 내역이 존재하지 않습니다.");
        }
        if (!savedCode.equals(inputCode)) {
            throw new IllegalArgumentException("인증 번호가 일치하지 않습니다.");
        }

        // 인증 완료 처리
        resetPasswordCodes.remove(email);
        verifiedResetEmails.put(email, true);
    }

    /**
     * [비밀번호 재설정 3단계: 새 비밀번호 변경 및 계정 잠금 해제]
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getSchoolEmail();

        // 1. 이메일 재인증 완료 여부 검증
        if (!Boolean.TRUE.equals(verifiedResetEmails.get(email))) {
            throw new IllegalStateException("학교 이메일 재인증이 완료되지 않았습니다.");
        }

        // 2. 유저 및 자격 증명 조회
        User user = userRepository.findBySchoolEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        LocalCredential credential = localCredentialRepository.findByUser(user)
                .orElseGet(() -> localCredentialRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new IllegalArgumentException("자체 로그인 자격 정보가 존재하지 않습니다.")));

        // 3. 새 비밀번호 암호화 저장 및 실패 카운트/잠금 초기화
        credential.updatePassword(passwordEncoder.encode(request.getNewPassword()));

        // 사용한 재인증 정보 제거
        verifiedResetEmails.remove(email);
    }
}
