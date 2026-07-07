package team2.goodsmap.store.dto.response;

import team2.goodsmap.store.entity.StoreGoods;

public record NewStoreGoodsResponse(
        Long id,
        Long storeId,
        Long goodsId,
        String goodsName,
        Long animationId,
        int price,
        int stock,
        String imagePath
) {
    public static NewStoreGoodsResponse from(StoreGoods storeGoods) {
        return new NewStoreGoodsResponse(
                storeGoods.getId(),
                storeGoods.getStore().getId(),
                storeGoods.getGoods().getId(),
                storeGoods.getGoods().getName(),
                storeGoods.getGoods().getAnimation().getId(),
                storeGoods.getPrice(),
                storeGoods.getStock(),
                storeGoods.getImagePath()
        );
    }
}
