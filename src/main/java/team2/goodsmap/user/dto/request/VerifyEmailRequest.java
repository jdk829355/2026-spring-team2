package team2.goodsmap.user.dto.request;

import lombok.Getter;

@Getter
public class VerifyEmailRequest {
    private String email;
    private String authCode;
}