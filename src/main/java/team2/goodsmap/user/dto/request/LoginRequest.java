package team2.goodsmap.user.dto.request;

import lombok.Getter;
import team2.goodsmap.user.enums.UserRole;

@Getter
public class LoginRequest {
    private String email;
    private String password;
    private UserRole role;
}