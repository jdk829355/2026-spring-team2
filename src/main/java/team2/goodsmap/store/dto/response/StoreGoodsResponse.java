package team2.goodsmap.store.dto.response;

import team2.goodsmap.goods.dto.GoodsResponse;
import team2.goodsmap.store.entity.StoreGoods;

public record StoreGoodsResponse(
        Long id,
        Long storeId,
        GoodsResponse goodsInfo,
        int price,
        int stock,
        String imagePath
) {
    public static StoreGoodsResponse from(StoreGoods storeGoods, String imagePath) {
        return new StoreGoodsResponse(
                storeGoods.getId(),
                storeGoods.getStore().getId(),
                GoodsResponse.from(storeGoods.getGoods()),
                storeGoods.getPrice(),
                storeGoods.getStock(),
                imagePath
        );
    }
}
