package team2.goodsmap.goods.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateGoodsRequest(
        @NotBlank Long animationId,
        @NotBlank String name
) {
}
