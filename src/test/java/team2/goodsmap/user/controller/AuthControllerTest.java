package team2.goodsmap.user.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import team2.goodsmap.user.dto.request.BusinessSignupRequest;
import team2.goodsmap.user.dto.request.LoginRequest;
import team2.goodsmap.user.dto.request.MemberSignupRequest;
import team2.goodsmap.user.dto.response.LoginResponse;
import team2.goodsmap.user.enums.UserRole;
import team2.goodsmap.user.service.AuthService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController 웹 계층 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Nested
    @DisplayName("개인 회원가입")
    class SignupMember {

        @Test
        @DisplayName("JSON 요청이 MemberSignupRequest로 정상 역직렬화되어 서비스에 전달된다")
        void requestBodyIsCorrectlyBound() throws Exception {
            String json = """
                    {
                      "name": "세현",
                      "email": "sehyeon@test.com",
                      "password": "pw1234"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/signup/member")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<MemberSignupRequest> captor = ArgumentCaptor.forClass(MemberSignupRequest.class);
            verify(authService).signupMember(captor.capture());

            MemberSignupRequest captured = captor.getValue();
            assertThat(captured.getName()).isEqualTo("세현");
            assertThat(captured.getEmail()).isEqualTo("sehyeon@test.com");
            assertThat(captured.getPassword()).isEqualTo("pw1234");
        }
    }

    @Nested
    @DisplayName("업체 회원가입")
    class SignupBusiness {

        @Test
        @DisplayName("JSON 요청이 BusinessSignupRequest로 정상 역직렬화되어 서비스에 전달된다")
        void requestBodyIsCorrectlyBound() throws Exception {
            String json = """
                    {
                      "name": "업체담당자",
                      "email": "biz@test.com",
                      "password": "pw1234"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/signup/business")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<BusinessSignupRequest> captor = ArgumentCaptor.forClass(BusinessSignupRequest.class);
            verify(authService).signupBusiness(captor.capture());

            BusinessSignupRequest captured = captor.getValue();
            assertThat(captured.getName()).isEqualTo("업체담당자");
            assertThat(captured.getEmail()).isEqualTo("biz@test.com");
            assertThat(captured.getPassword()).isEqualTo("pw1234");
        }
    }

    @Nested
    @DisplayName("이메일 중복 확인")
    class CheckEmail {

        @Test
        @DisplayName("쿼리 파라미터로 받은 이메일을 그대로 서비스에 전달한다")
        void passesEmailParam() throws Exception {
            when(authService.checkEmail("dup@test.com")).thenReturn(true);

            mockMvc.perform(get("/api/v1/auth/check-email").param("email", "dup@test.com"))
                    .andExpect(status().isOk());

            verify(authService).checkEmail("dup@test.com");
        }
    }

    @Nested
    @DisplayName("이메일 인증")
    class VerifyEmail {

        @Test
        @DisplayName("JSON 요청의 email/authCode가 서비스에 정상 전달된다")
        void requestBodyIsCorrectlyBound() throws Exception {
            String json = """
                    {
                      "email": "verify@test.com",
                      "authCode": "123456"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());

            verify(authService).verifyEmail("verify@test.com", "123456");
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("개인 로그인 요청이 LoginRequest로 정상 역직렬화되어 서비스에 전달된다")
        void memberLogin_requestBodyIsCorrectlyBound() throws Exception {
            when(authService.login(any())).thenReturn(LoginResponse.of("access-token", "refresh-token"));

            String json = """
                    {
                      "email": "login@test.com",
                      "password": "pw1234",
                      "role": "USER"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/login/member")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());

            ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
            verify(authService).login(captor.capture());

            LoginRequest captured = captor.getValue();
            assertThat(captured.getEmail()).isEqualTo("login@test.com");
            assertThat(captured.getPassword()).isEqualTo("pw1234");
            assertThat(captured.getRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("업체 로그인도 같은 login() 서비스 메서드를 호출한다")
        void businessLogin_callsSameServiceMethod() throws Exception {
            when(authService.login(any())).thenReturn(LoginResponse.of("access-token", "refresh-token"));

            String json = """
                    {
                      "email": "biz@test.com",
                      "password": "pw1234",
                      "role": "STORE"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/login/business")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());

            verify(authService).login(any());
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Reissue {

        @Test
        @DisplayName("JSON의 refreshToken이 서비스에 정상 전달된다")
        void requestBodyIsCorrectlyBound() throws Exception {
            when(authService.reissue("refresh-abc")).thenReturn(LoginResponse.of("new-access", "new-refresh"));

            String json = """
                    {
                      "refreshToken": "refresh-abc"
                    }
                    """;

            mockMvc.perform(post("/api/v1/auth/reissue")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());

            verify(authService).reissue("refresh-abc");
        }
    }
}