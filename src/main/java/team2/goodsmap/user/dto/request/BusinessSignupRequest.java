package team2.goodsmap.user.dto.request;

import lombok.Getter;
import team2.goodsmap.store.enums.StoreType;

@Getter
public class BusinessSignupRequest {
    private String name;        // 업체명
    private String email;
    private String password;
}