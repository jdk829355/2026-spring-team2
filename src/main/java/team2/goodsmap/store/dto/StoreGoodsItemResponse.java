package team2.goodsmap.store.dto;

import lombok.Builder;
import lombok.Getter;
import team2.goodsmap.store.entity.StoreGoods;

@Getter
@Builder
public class StoreGoodsItemResponse {

    private Long storeGoodsId;
    private Long goodsId;
    private String goodsName;
    private String animationTitle;
    private int price;
    private int stock;
    private String imagePath;

    public static StoreGoodsItemResponse from(StoreGoods storeGoods) {
        return StoreGoodsItemResponse.builder()
                .storeGoodsId(storeGoods.getId())
                .goodsId(storeGoods.getGoods().getId())
                .goodsName(storeGoods.getGoods().getName())
                .animationTitle(storeGoods.getGoods().getAnimation().getTitle())
                .price(storeGoods.getPrice())
                .stock(storeGoods.getStock())
                .imagePath(storeGoods.getImagePath())
                .build();
    }
}
