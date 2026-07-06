package team2.goodsmap.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;

    public static LoginResponse of(String accessToken, String refreshToken) {
        return new LoginResponse(accessToken, refreshToken);
    }
}