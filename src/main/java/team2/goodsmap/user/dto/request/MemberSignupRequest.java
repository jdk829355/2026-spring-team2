package team2.goodsmap.user.dto.request;

import lombok.Getter;

@Getter
public class MemberSignupRequest {
    private String name;
    private String email;
    private String password;
}