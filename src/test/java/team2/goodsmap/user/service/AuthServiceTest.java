package team2.goodsmap.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import team2.goodsmap.global.jwt.JwtTokenProvider;
import team2.goodsmap.user.dto.request.BusinessSignupRequest;
import team2.goodsmap.user.dto.request.LoginRequest;
import team2.goodsmap.user.dto.request.MemberSignupRequest;
import team2.goodsmap.user.dto.response.LoginResponse;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.enums.UserRole;
import team2.goodsmap.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("개인 회원가입")
    class SignupMember {

        @Test
        @DisplayName("신규 이메일이면 정상 가입되고 인증 메일이 발송된다")
        void success() {
            // given
            MemberSignupRequest request = memberSignupRequest("세현", "sehyeon@test.com", "pw1234");
            when(userRepository.findByEmail("sehyeon@test.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("pw1234")).thenReturn("encodedPw");
            when(emailService.generateAuthCode()).thenReturn("123456");

            // when
            authService.signupMember(request);

            // then
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User saved = captor.getValue();
            assertThat(saved.getEmail()).isEqualTo("sehyeon@test.com");
            assertThat(saved.getPassword()).isEqualTo("encodedPw");
            assertThat(saved.getRole()).isEqualTo(UserRole.USER);
            assertThat(saved.getAuthCode()).isEqualTo("123456");
            verify(emailService).sendAuthCode("sehyeon@test.com", "123456");
        }

        @Test
        @DisplayName("이미 인증완료된 이메일이면 가입이 거부된다")
        void alreadyVerifiedEmail() {
            // given
            User existing = User.builder()
                    .name("기존")
                    .email("dup@test.com")
                    .password("pw")
                    .role(UserRole.USER)
                    .build();
            existing.verify();

            when(userRepository.findByEmail("dup@test.com")).thenReturn(Optional.of(existing));

            MemberSignupRequest request = memberSignupRequest("새유저", "dup@test.com", "pw1234");

            // when & then
            assertThatThrownBy(() -> authService.signupMember(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 사용 중인 이메일입니다.");

            verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
            verify(userRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("인증 대기 중(만료 전)인 이메일이면 가입이 거부된다")
        void pendingNotExpired() {
            // given
            User pending = User.builder()
                    .name("대기중")
                    .email("pending@test.com")
                    .password("pw")
                    .role(UserRole.USER)
                    .build();
            pending.setAuthCode("999999", LocalDateTime.now().plusMinutes(1));

            when(userRepository.findByEmail("pending@test.com")).thenReturn(Optional.of(pending));

            MemberSignupRequest request = memberSignupRequest("새유저", "pending@test.com", "pw1234");

            // when & then
            assertThatThrownBy(() -> authService.signupMember(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 인증 대기 중인 이메일입니다. 잠시 후 다시 시도해주세요.");

            verify(userRepository, never()).delete(org.mockito.ArgumentMatchers.any());
            verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
        }

        @Test
        @DisplayName("인증코드가 만료된 미인증 계정이면 삭제 후 재가입이 허용된다")
        void expiredPendingAllowsRetry() {
            // given
            User expired = User.builder()
                    .name("만료됨")
                    .email("expired@test.com")
                    .password("pw")
                    .role(UserRole.USER)
                    .build();
            expired.setAuthCode("111111", LocalDateTime.now().minusMinutes(1));

            when(userRepository.findByEmail("expired@test.com")).thenReturn(Optional.of(expired));
            when(passwordEncoder.encode("newpw")).thenReturn("encodedNewPw");
            when(emailService.generateAuthCode()).thenReturn("222222");

            MemberSignupRequest request = memberSignupRequest("재시도", "expired@test.com", "newpw");

            // when
            authService.signupMember(request);

            // then
            verify(userRepository).delete(expired);
            verify(userRepository).save(org.mockito.ArgumentMatchers.any(User.class));
            verify(emailService).sendAuthCode("expired@test.com", "222222");
        }
    }

    @Nested
    @DisplayName("업체 회원가입")
    class SignupBusiness {

        @Test
        @DisplayName("정상 가입되면 role이 STORE로 저장된다")
        void success() {
            // given
            BusinessSignupRequest request = businessSignupRequest("업체담당자", "biz@test.com", "pw1234");
            when(userRepository.findByEmail("biz@test.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("pw1234")).thenReturn("encodedPw");
            when(emailService.generateAuthCode()).thenReturn("333333");

            // when
            authService.signupBusiness(request);

            // then
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(UserRole.STORE);
            verify(emailService).sendAuthCode("biz@test.com", "333333");
        }

        @Test
        @DisplayName("이미 사용 중인 이메일이면 가입이 거부된다")
        void duplicateEmail() {
            // given
            User existing = User.builder()
                    .name("기존업체")
                    .email("dupbiz@test.com")
                    .password("pw")
                    .role(UserRole.STORE)
                    .build();
            existing.verify();

            when(userRepository.findByEmail("dupbiz@test.com")).thenReturn(Optional.of(existing));

            BusinessSignupRequest request = businessSignupRequest("새업체", "dupbiz@test.com", "pw1234");

            // when & then
            assertThatThrownBy(() -> authService.signupBusiness(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 사용 중인 이메일입니다.");
        }
    }

    @Nested
    @DisplayName("이메일 중복 확인")
    class CheckEmail {

        @Test
        @DisplayName("repository 결과를 그대로 반환한다")
        void delegatesToRepository() {
            when(userRepository.existsByEmail("a@test.com")).thenReturn(true);

            assertThat(authService.checkEmail("a@test.com")).isTrue();
        }
    }

    @Nested
    @DisplayName("이메일 인증")
    class VerifyEmail {

        @Test
        @DisplayName("올바른 인증코드면 인증 처리된다")
        void success() {
            // given
            User user = User.builder()
                    .name("인증대상")
                    .email("verify@test.com")
                    .password("pw")
                    .role(UserRole.USER)
                    .build();
            user.setAuthCode("123456", LocalDateTime.now().plusMinutes(1));
            when(userRepository.findByEmail("verify@test.com")).thenReturn(Optional.of(user));

            // when
            authService.verifyEmail("verify@test.com", "123456");

            // then
            assertThat(user.isVerified()).isTrue();
            assertThat(user.getAuthCode()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 예외가 발생한다")
        void userNotFound() {
            when(userRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail("none@test.com", "123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 사용자입니다.");
        }

        @Test
        @DisplayName("인증코드를 요청한 적이 없으면 예외가 발생한다")
        void authCodeNotRequested() {
            User user = User.builder()
                    .name("미요청")
                    .email("norequest@test.com")
                    .password("pw")
                    .role(UserRole.USER)
                    .build();
            when(userRepository.findByEmail("norequest@test.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyEmail("norequest@test.com", "123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증번호를 먼저 요청해주세요.");
        }

        @Test
        @DisplayName("인증코드가 만료되었으면 예외가 발생한다")
        void expired() {
            User user = User.builder()
                    .name("만료")
                    .email("expired2@test.com")
                    .password("pw")
                    .role(UserRole.USER)
                    .build();
            user.setAuthCode("123456", LocalDateTime.now().minusSeconds(1));
            when(userRepository.findByEmail("expired2@test.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyEmail("expired2@test.com", "123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증번호가 만료되었습니다.");
        }

        @Test
        @DisplayName("인증코드가 일치하지 않으면 예외가 발생한다")
        void wrongCode() {
            User user = User.builder()
                    .name("불일치")
                    .email("wrong@test.com")
                    .password("pw")
                    .role(UserRole.USER)
                    .build();
            user.setAuthCode("123456", LocalDateTime.now().plusMinutes(1));
            when(userRepository.findByEmail("wrong@test.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyEmail("wrong@test.com", "000000"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증번호가 올바르지 않습니다.");
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("정상 로그인 시 토큰이 발급된다")
        void success() {
            // given
            User user = User.builder()
                    .name("로그인")
                    .email("login@test.com")
                    .password("encodedPw")
                    .role(UserRole.USER)
                    .build();
            user.verify();
            ReflectionTestUtils.setField(user, "id", 1L);

            when(userRepository.findByEmailAndRole("login@test.com", UserRole.USER))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("rawPw", "encodedPw")).thenReturn(true);
            when(jwtTokenProvider.generateAccessToken(1L, "login@test.com", "USER"))
                    .thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(1L)).thenReturn("refresh-token");

            LoginRequest request = loginRequest("login@test.com", "rawPw", UserRole.USER);

            // when
            LoginResponse response = authService.login(request);

            // then
            assertThat(response).isNotNull();
            verify(jwtTokenProvider).generateAccessToken(1L, "login@test.com", "USER");
            verify(jwtTokenProvider).generateRefreshToken(1L);
        }

        @Test
        @DisplayName("존재하지 않는 계정이면 예외가 발생한다")
        void userNotFound() {
            when(userRepository.findByEmailAndRole("none@test.com", UserRole.USER))
                    .thenReturn(Optional.empty());

            LoginRequest request = loginRequest("none@test.com", "pw", UserRole.USER);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        @Test
        @DisplayName("이메일 미인증 계정이면 예외가 발생한다")
        void notVerified() {
            User user = User.builder()
                    .name("미인증")
                    .email("notverified@test.com")
                    .password("pw")
                    .role(UserRole.USER)
                    .build();
            when(userRepository.findByEmailAndRole("notverified@test.com", UserRole.USER))
                    .thenReturn(Optional.of(user));

            LoginRequest request = loginRequest("notverified@test.com", "pw", UserRole.USER);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이메일 인증이 완료되지 않은 계정입니다.");
        }

        @Test
        @DisplayName("비밀번호가 틀리면 예외가 발생한다")
        void wrongPassword() {
            User user = User.builder()
                    .name("비번틀림")
                    .email("wrongpw@test.com")
                    .password("encodedPw")
                    .role(UserRole.USER)
                    .build();
            user.verify();
            when(userRepository.findByEmailAndRole("wrongpw@test.com", UserRole.USER))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongRaw", "encodedPw")).thenReturn(false);

            LoginRequest request = loginRequest("wrongpw@test.com", "wrongRaw", UserRole.USER);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Reissue {

        @Test
        @DisplayName("유효한 refresh token이면 새 토큰이 발급된다")
        void success() {
            User user = User.builder()
                    .name("재발급")
                    .email("reissue@test.com")
                    .password("pw")
                    .role(UserRole.USER)
                    .build();
            ReflectionTestUtils.setField(user, "id", 5L);

            when(jwtTokenProvider.validateToken("valid-refresh")).thenReturn(true);
            when(jwtTokenProvider.getUserId("valid-refresh")).thenReturn(5L);
            when(userRepository.findById(5L)).thenReturn(Optional.of(user));
            when(jwtTokenProvider.generateAccessToken(5L, "reissue@test.com", "USER"))
                    .thenReturn("new-access");
            when(jwtTokenProvider.generateRefreshToken(5L)).thenReturn("new-refresh");

            LoginResponse response = authService.reissue("valid-refresh");

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("유효하지 않은 토큰이면 예외가 발생한다")
        void invalidToken() {
            when(jwtTokenProvider.validateToken("invalid")).thenReturn(false);

            assertThatThrownBy(() -> authService.reissue("invalid"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("유효하지 않은 Refresh Token입니다.");
        }

        @Test
        @DisplayName("토큰은 유효하지만 사용자가 없으면 예외가 발생한다")
        void userNotFound() {
            when(jwtTokenProvider.validateToken("valid-but-no-user")).thenReturn(true);
            when(jwtTokenProvider.getUserId("valid-but-no-user")).thenReturn(99L);
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.reissue("valid-but-no-user"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 사용자입니다.");
        }
    }

    // ---------- 테스트용 DTO 생성 헬퍼 ----------

    private MemberSignupRequest memberSignupRequest(String name, String email, String password) {
        MemberSignupRequest request = new MemberSignupRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private BusinessSignupRequest businessSignupRequest(String name, String email, String password) {
        BusinessSignupRequest request = new BusinessSignupRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private LoginRequest loginRequest(String email, String password, UserRole role) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", password);
        ReflectionTestUtils.setField(request, "role", role);
        return request;
    }
}