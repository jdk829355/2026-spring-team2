package team2.goodsmap.store.dto.response;

import team2.goodsmap.store.entity.StoreGoods;

public record StoreGoodsItemResponse(
        Long storeGoodsId,
        Long goodsId,
        String goodsName,
        String animationTitle,
        int price,
        int stock,
        String imagePath
) {
    public static StoreGoodsItemResponse from(StoreGoods storeGoods, String imagePath) {
        return new StoreGoodsItemResponse(
                storeGoods.getId(),
                storeGoods.getGoods().getId(),
                storeGoods.getGoods().getName(),
                storeGoods.getGoods().getAnimation().getTitle(),
                storeGoods.getPrice(),
                storeGoods.getStock(),
                imagePath
        );
    }
}
