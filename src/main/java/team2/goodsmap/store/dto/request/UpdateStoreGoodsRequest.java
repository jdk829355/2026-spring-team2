package team2.goodsmap.store.dto.request;

import jakarta.validation.constraints.Min;

public record UpdateStoreGoodsRequest(
        @Min(0) Integer price,
        @Min(0) Integer stock,
        String imagePath
) {
}
