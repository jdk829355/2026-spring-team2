package team2.goodsmap.store.dto.request;

import jakarta.validation.constraints.Min;
import team2.goodsmap.goods.dto.CreateGoodsRequest;

public record AddNewStoreGoodsRequest(
        CreateGoodsRequest goodsInfo,
        @Min(0) int price,
        @Min(0) int stock,
        String imagePath
) {
}
