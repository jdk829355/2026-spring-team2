package team2.goodsmap.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddImagePathRequest(
        @NotBlank(message = "objectKey는 필수입니다.")
        @Pattern(
                regexp = "^stores/[1-9]\\d*/goods/[1-9]\\d*/images/"
                        + "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-"
                        + "[89ab][0-9a-f]{3}-[0-9a-f]{12}"
                        + "\\.(jpg|jpeg|png|webp)$",
                message = "올바르지 않은 이미지 objectKey 형식입니다."
        )
        String imagePath
) {
}