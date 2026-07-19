package team2.goodsmap.planner.dto.response;

import lombok.Builder;
import lombok.Getter;
import team2.goodsmap.goods.entity.Goods;
import team2.goodsmap.planner.entity.PlannerGoods;
import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.entity.StoreGoods;

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

    // 엔티티 → DTO 변환. PlannerResponse.from() 과 같은 패턴.
    public static PlannerGoodsResponse from(PlannerGoods plannerGoods, String imagePath) {
        StoreGoods storeGoods = plannerGoods.getStoreGoods();
        Goods goods = storeGoods.getGoods();
        Store store = storeGoods.getStore();

        return PlannerGoodsResponse.builder()
                .id(plannerGoods.getId())
                .storeGoodsId(storeGoods.getId())
                .goodsName(goods.getName())
                .animationTitle(goods.getAnimation().getTitle())
                .price(storeGoods.getPrice())
                .stock(storeGoods.getStock())
                .imagePath(imagePath)
                .store(StoreInfo.builder()
                        .id(store.getId())
                        .name(store.getName())
                        .address(store.getAddress())
                        .build())
                .build();
    }

    @Getter
    @Builder
    public static class StoreInfo {
        private Long id;
        private String name;
        private String address;
    }
}
