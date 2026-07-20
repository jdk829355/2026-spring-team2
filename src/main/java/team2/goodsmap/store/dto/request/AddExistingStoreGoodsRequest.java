package team2.goodsmap.store.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddExistingStoreGoodsRequest(
        @NotNull(message = "goodsId는 필수입니다.")
        @Positive(message = "goodsId는 양수여야 합니다.")
        Long goodsId,
        @NotNull(message = "가격은 필수입니다.")
        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        Integer price,
        @NotNull(message = "재고는 필수입니다.")
        @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
        Integer stock
) {
}
