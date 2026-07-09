package team2.goodsmap.planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team2.goodsmap.planner.entity.PlannerGoods;

import java.util.List;

public interface PlannerGoodsRepository extends JpaRepository<PlannerGoods, Long> {
    // 특정 플래너에 담긴 굿즈 수
    long countByPlannerId(Long plannerId);

    // 특정 플래너에 담긴 굿즈 목록 (추가)
    List<PlannerGoods> findByPlannerId(Long plannerId);
}
