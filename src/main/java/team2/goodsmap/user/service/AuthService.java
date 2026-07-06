package team2.goodsmap.user.service;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 개인 회원가입 요청
    @Transactional
    public void signupMember(MemberSignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .build();

        userRepository.save(user);
        // TODO: 이메일 인증번호 발송
    }

    // 업체 회원가입 요청
    @Transactional
    public void signupBusiness(BusinessSignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.STORE)
                .build();

        userRepository.save(user);
        // TODO: store 생성 + store_admin 연결 + 이메일 인증번호 발송
    }

    // 이메일 중복 확인
    public boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // 개인/업체 로그인
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndRole(request.getEmail(), request.getRole())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        /*
        if (!user.isVerified()) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않은 계정입니다.");
        }
        */

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

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

        return LoginResponse.of(newAccessToken, newRefreshToken);
    }

    // 로그아웃
    public void logout() {

    }
}