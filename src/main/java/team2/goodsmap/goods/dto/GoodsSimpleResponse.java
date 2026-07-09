package team2.goodsmap.goods.dto;

import lombok.Builder;
import lombok.Getter;
import team2.goodsmap.goods.entity.Goods;

@Getter
@Builder
public class GoodsSimpleResponse {

    private Long id;
    private String name;
    private Long animationId;
    private String animationTitle;

    public static GoodsSimpleResponse from(Goods goods) {
        return GoodsSimpleResponse.builder()
                .id(goods.getId())
                .name(goods.getName())
                .animationId(goods.getAnimation().getId())
                .animationTitle(goods.getAnimation().getTitle())
                .build();
    }
}
