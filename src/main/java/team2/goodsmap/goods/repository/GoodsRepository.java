package team2.goodsmap.goods.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team2.goodsmap.goods.entity.Goods;

import java.util.List;

public interface GoodsRepository extends JpaRepository<Goods, Long> {

    // 상품 목록 조회 (등록용) - GET /api/v1/goods?q
    List<Goods> findByNameContainingIgnoreCase(String q);

    // 상품 목록 조회 (탐색용) - GET /api/v1/goods/search
    // Goods와 Store는 직접 연관이 없어 StoreGoods를 거치는 서브쿼리로 지역 필터링
    @Query("""
        SELECT DISTINCT g FROM Goods g
        JOIN g.animation a
        WHERE (:animationId IS NULL OR a.id = :animationId)
          AND (:keyword IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:region IS NULL OR g.id IN (
                SELECT sg.goods.id FROM StoreGoods sg
                WHERE sg.store.address LIKE CONCAT('%', :region, '%')
          ))
        """)
    List<Goods> searchGoods(@Param("animationId") Long animationId,
                            @Param("region") String region,
                            @Param("keyword") String keyword);
}
