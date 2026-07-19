package team2.goodsmap.goods.repository;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team2.goodsmap.goods.entity.Goods;
import org.springframework.data.jpa.repository.EntityGraph;
import team2.goodsmap.goods.dto.GoodsSearchRow;

import java.util.List;

public interface GoodsRepository extends JpaRepository<Goods, Long> {

    // findAll() 재선언 — 기본 구현 대신 이 시그니처가 우선 적용됨
    @EntityGraph(attributePaths = {"animation"})
    List<Goods> findAll();

    // 상품 목록 조회 (등록용) - GET /api/v1/goods?q
    @EntityGraph(attributePaths = {"animation"})
    List<Goods> findByNameContainingIgnoreCase(String q);

    // 상품 목록 조회 (탐색용) - GET /api/v1/goods/search
    // StoreGoods를 조인해 지역 필터에 해당하는 업체의 이미지 경로까지 함께 조회
    @Query("""
        SELECT new team2.goodsmap.goods.dto.GoodsSearchRow(
            g.id, g.name, a.id, a.title, sg.imagePath
        )
        FROM Goods g
        JOIN g.animation a
        LEFT JOIN StoreGoods sg ON sg.goods = g
        LEFT JOIN sg.store s
        WHERE (:animationId IS NULL OR a.id = :animationId)
          AND (:keyword IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
          AND (:region IS NULL OR s.address LIKE CONCAT('%', CAST(:region AS string), '%'))
        ORDER BY g.id, sg.id
        """)
    List<GoodsSearchRow> searchGoods(@Param("animationId") Long animationId,
                                     @Param("region") String region,
                                     @Param("keyword") String keyword);

    boolean existsByName(@NotBlank String name);


}

