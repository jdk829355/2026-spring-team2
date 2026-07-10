package team2.goodsmap.store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.UUID;

public record AddStoreAdminRequest(
        @Email(message = "유효한 이메일 주소를 입력해주세요.") @NotBlank(message = "이메일은 필수입니다.")
        String email
) {
}
