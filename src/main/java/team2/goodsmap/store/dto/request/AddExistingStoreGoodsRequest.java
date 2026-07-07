package team2.goodsmap.store.dto.request;

public record AddExistingStoreGoodsRequest(
        Long goodsId,
        int price,
        int stock,
        String imagePath
) {
}
