package team2.goodsmap.store.dto.request;

public record AddNewStoreGoodsRequest(
        Long animationId,
        String goodsName,
        int price,
        int stock,
        String imagePath
) {
}
