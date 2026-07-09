package team2.goodsmap.planner.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlannerGoodsResponse {
    private Long id;              // planner_goods.id
    private Long storeGoodsId;
    private String goodsName;
    private String animationTitle;
    private int price;
    private int stock;
    private String imagePath;
    private StoreInfo store;

    @Getter
    @Builder
    public static class StoreInfo {
        private Long id;
        private String name;
        private String address;
    }
}