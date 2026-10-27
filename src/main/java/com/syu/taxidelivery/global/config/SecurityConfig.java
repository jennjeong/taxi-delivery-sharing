package com.syu.taxidelivery.global.config;

import com.syu.taxidelivery.global.jwt.JwtAuthenticationFilter;
import com.syu.taxidelivery.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * [Spring Security 종합 보안 설정 클래스]
 * - 세션 기반 인증 (SessionCreationPolicy.IF_REQUIRED)
 * - CSRF 쿠키 토큰 저장소 연동
 * - 계정 권한(STUDENT, ADMIN)별 URL 인가 검증
 * - 로그아웃 및 예외 처리
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 처리: 브라우저/JS 클라이언트가 읽을 수 있도록 XSRF-TOKEN 쿠키 발급, 공개 인증 API는 제외
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/auth/**", "/h2-console/**")
                )

                // 2. 기본 로그인 폼 및 HTTP Basic 비활성화 (커스텀 세션 로그인 API 사용)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // 3. 세션 관리: 세션 인증 지원을 위해 필요 시 세션 생성
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1) // 동시 세션 1개 제한
                )

                // 4. URL별 인가 및 계정 권한 검증
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/h2-console/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/student/**", "/api/party/**").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers("/api/service/**", "/service/**").authenticated() // 인증된 서비스 화면/API
                        .anyRequest().authenticated()
                )

                // 5. 로그아웃 설정: 세션 무효화 및 JSESSIONID 삭제
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\": \"로그아웃되었습니다.\", \"redirectUrl\": \"/login\"}");
                        })
                )

                // 6. 예외 핸들링 (401 미인증, 403 권한 부족)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\": \"UNAUTHORIZED\", \"message\": \"인증이 필요합니다.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\": \"FORBIDDEN\", \"message\": \"접근 권한이 없습니다.\"}");
                        })
                )

                // 7. JWT 헤더가 들어오는 경우를 위한 필터 (세션과 JWT 동시 호환)
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
