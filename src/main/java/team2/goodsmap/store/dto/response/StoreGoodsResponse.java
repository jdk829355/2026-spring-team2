package team2.goodsmap.store.dto.response;

import team2.goodsmap.store.entity.StoreGoods;

public record StoreGoodsResponse(
        Long id,
        Long storeId,
        Long goodsId,
        String goodsName,
        int price,
        int stock,
        String imagePath
) {
    public static StoreGoodsResponse from(StoreGoods storeGoods) {
        return new StoreGoodsResponse(
                storeGoods.getId(),
                storeGoods.getStore().getId(),
                storeGoods.getGoods().getId(),
                storeGoods.getGoods().getName(),
                storeGoods.getPrice(),
                storeGoods.getStock(),
                storeGoods.getImagePath()
        );
    }
}
