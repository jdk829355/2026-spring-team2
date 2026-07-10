package team2.goodsmap.store.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddStoreGoodsRequest(
        @NotBlank @Min(0) int price,
        @NotBlank @Min(0) int stock,
        String imagePath
) {
}
