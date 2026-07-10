package team2.goodsmap.store.dto.request;

public record UpdateStoreGoodsRequest(
        Integer price,
        Integer stock,
        String imagePath
) {
}
