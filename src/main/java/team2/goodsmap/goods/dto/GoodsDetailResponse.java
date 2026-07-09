package team2.goodsmap.goods.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GoodsDetailResponse {

    private Long id;
    private String name;
    private Long animationId;
    private String animationTitle;
    private List<StoreSummary> stores;

    @Getter
    @Builder
    public static class StoreSummary {
        private Long storeGoodsId;
        private Long storeId;
        private String storeName;
        private String address;
        private Integer price;
        private Integer stock;
        private String imagePath;
    }
}
