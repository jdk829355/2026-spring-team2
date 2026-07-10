package team2.goodsmap.planner.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team2.goodsmap.store.entity.StoreGoods;
import lombok.Builder;

@Entity
@Table(name = "planner_goods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlannerGoods {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_goods_id", nullable = false)
    private StoreGoods storeGoods;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planner_id", nullable = false)
    private Planner planner;

    @Builder //'내가 살 것 담기'에 추가 시 사용해야
    public PlannerGoods(StoreGoods storeGoods, Planner planner) {
        this.storeGoods = storeGoods;
        this.planner = planner;
    }
}