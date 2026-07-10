package team2.goodsmap.planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team2.goodsmap.planner.entity.PlannerGoods;

public interface PlannerGoodsRepository extends JpaRepository<PlannerGoods, Long> {
}
