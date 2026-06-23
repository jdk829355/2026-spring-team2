package team2.goodsmap.planner.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team2.goodsmap.store.entity.StoreGoods;

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
}