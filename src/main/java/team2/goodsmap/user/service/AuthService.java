package team2.goodsmap.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.global.jwt.JwtTokenProvider;
import team2.goodsmap.user.dto.request.LoginRequest;
import team2.goodsmap.user.dto.request.MemberSignupRequest;
import team2.goodsmap.user.dto.request.BusinessSignupRequest;
import team2.goodsmap.user.dto.response.LoginResponse;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.enums.UserRole;
import team2.goodsmap.user.repository.UserRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    // 개인 회원가입 요청
    @Transactional
    public void signupMember(MemberSignupRequest request) {
        validateAndCleanupExpiredEmail(request.getEmail());

        String authCode = emailService.generateAuthCode();

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .build();

        user.setAuthCode(authCode, LocalDateTime.now().plusMinutes(2));
        userRepository.save(user);
        emailService.sendAuthCode(request.getEmail(), authCode);
        log.atInfo()
                .addKeyValue("event", "MEMBER_SIGNUP_REQUESTED")
                .addKeyValue("userId", user.getId())
                .addKeyValue("email", maskEmail(request.getEmail()))
                .log("개인 회원가입 요청");
    }

    // 업체 회원가입 요청
    @Transactional
    public void signupBusiness(BusinessSignupRequest request) {
        validateAndCleanupExpiredEmail(request.getEmail());

        String authCode = emailService.generateAuthCode();

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.STORE)
                .build();

        user.setAuthCode(authCode, LocalDateTime.now().plusMinutes(2));
        userRepository.save(user);
        emailService.sendAuthCode(request.getEmail(), authCode);
        log.atInfo()
                .addKeyValue("event", "BUSINESS_SIGNUP_REQUESTED")
                .addKeyValue("userId", user.getId())
                .addKeyValue("email", maskEmail(request.getEmail()))
                .log("업체 회원가입 요청");
    }

    // 이메일 중복 확인
    public boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public void verifyEmail(String email, String authCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (user.getAuthCode() == null) {
            throw new IllegalArgumentException("인증번호를 먼저 요청해주세요.");
        }

        if (LocalDateTime.now().isAfter(user.getAuthCodeExpiredAt())) {
            throw new IllegalArgumentException("인증번호가 만료되었습니다.");
        }

        if (!user.getAuthCode().equals(authCode)) {
            throw new IllegalArgumentException("인증번호가 올바르지 않습니다.");
        }

        user.verify();
        log.atInfo()
                .addKeyValue("event", "EMAIL_VERIFIED")
                .addKeyValue("userId", user.getId())
                .addKeyValue("email", maskEmail(email))
                .log("이메일 인증 완료");
    }

    private void validateAndCleanupExpiredEmail(String email) {
        userRepository.findByEmail(email).ifPresent(existingUser -> {
            if (existingUser.isVerified()) {
                throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
            }
            if (existingUser.getAuthCodeExpiredAt() != null
                    && LocalDateTime.now().isBefore(existingUser.getAuthCodeExpiredAt())) {
                throw new IllegalArgumentException("이미 인증 대기 중인 이메일입니다. 잠시 후 다시 시도해주세요.");
            }
            // 인증 안 됐고, 인증코드도 만료됨 → 즉시 재가입 허용
            userRepository.delete(existingUser);
        });
    }

    // 개인/업체 로그인
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndRole(request.getEmail(), request.getRole())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!user.isVerified()) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않은 계정입니다.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        log.atInfo()
                .addKeyValue("event", "USER_LOGGED_IN")
                .addKeyValue("userId", user.getId())
                .addKeyValue("role", user.getRole().name())
                .log("로그인");
        return LoginResponse.of(accessToken, refreshToken);
    }

    // 토큰 재발급
    public LoginResponse reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        log.atInfo()
                .addKeyValue("event", "TOKEN_REISSUED")
                .addKeyValue("userId", userId)
                .log("토큰 재발급");
        return LoginResponse.of(newAccessToken, newRefreshToken);
    }

    // 이메일 마스킹: toto@gmail.com -> to***@gmail.com
    // 로그에 개인정보(전체 이메일)가 평문으로 남지 않도록.
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String head = local.length() <= 2 ? local : local.substring(0, 2);
        return head + "***" + domain;
    }
}
