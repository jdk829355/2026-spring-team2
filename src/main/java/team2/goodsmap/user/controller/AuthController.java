package team2.goodsmap.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team2.goodsmap.global.common.ApiResponse;
import team2.goodsmap.user.dto.request.*;
import team2.goodsmap.user.dto.response.LoginResponse;
import team2.goodsmap.user.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 개인 회원가입 요청
    @PostMapping("/signup/member")
    public ResponseEntity<ApiResponse<Void>> signupMember(@RequestBody MemberSignupRequest request) {
        authService.signupMember(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 업체 회원가입 요청
    @PostMapping("/signup/business")
    public ResponseEntity<ApiResponse<Void>> signupBusiness(@RequestBody BusinessSignupRequest request) {
        authService.signupBusiness(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 이메일 중복 확인
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam String email) {
        boolean isDuplicated = authService.checkEmail(email);
        return ResponseEntity.ok(ApiResponse.success(isDuplicated));
    }

    // 이메일 인증번호 확인
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.getEmail(), request.getAuthCode());
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 개인 로그인
    @PostMapping("/login/member")
    public ResponseEntity<ApiResponse<LoginResponse>> loginMember(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 업체 로그인
    @PostMapping("/login/business")
    public ResponseEntity<ApiResponse<LoginResponse>> loginBusiness(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<LoginResponse>> reissue(@RequestBody ReissueRequest request) {
        LoginResponse response = authService.reissue(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}