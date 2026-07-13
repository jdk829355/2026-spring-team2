package team2.goodsmap.planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team2.goodsmap.planner.entity.PlannerGoods;

import java.util.List;

public interface PlannerGoodsRepository extends JpaRepository<PlannerGoods, Long> {

    // 특정 플래너에 담긴 굿즈 수
    long countByPlannerId(Long plannerId);

    // 특정 플래너에 담긴 굿즈 목록
    List<PlannerGoods> findByPlannerId(Long plannerId);

    // 상세 조회용: 굿즈 하나당 store_goods → goods → animation, store 를 전부 타고 들어가야 하므로
    // 그냥 findByPlannerId를 쓰면 굿즈 N개당 쿼리가 N번씩 더 나간다(N+1 문제).
    // JOIN FETCH로 한 방에 다 끌고 온다.
    @Query("""
            select pg from PlannerGoods pg
            join fetch pg.storeGoods sg
            join fetch sg.goods g
            join fetch g.animation
            join fetch sg.store
            where pg.planner.id = :plannerId
            """)
    List<PlannerGoods> findByPlannerIdWithDetails(@Param("plannerId") Long plannerId);

    // 특정 플래너에 속한 굿즈인지 확인용
    boolean existsByIdAndPlannerId(Long id, Long plannerId);
}
