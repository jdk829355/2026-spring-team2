package team2.goodsmap.goods.dto;

import jakarta.validation.constraints.NotBlank;
import team2.goodsmap.goods.entity.Goods;

public record GoodsResponse(
        Long id,
        String name,
        Long animationId,
        String animationName
) {
    public static GoodsResponse from(Goods save) {
        return new GoodsResponse(
                save.getId(),
                save.getName(),
                save.getAnimation().getId(),
                save.getAnimation().getTitle()
        );
    }
}
