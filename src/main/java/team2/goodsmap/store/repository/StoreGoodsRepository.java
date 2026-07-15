package team2.goodsmap.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team2.goodsmap.store.entity.StoreGoods;

import java.util.List;
import java.util.Optional;

public interface StoreGoodsRepository extends JpaRepository<StoreGoods, Long> {

    // 매장별 전체 재고 목록 조회
    // TODO: N+1 문제 있음. storeGoods 개수가 많아지면 goods/animation lazy loading 때문에 쿼리가 N번 더 나감.
// 나중에 findByStoreId 쿼리를 JOIN FETCH로 바꿀 것. -> 수정함(세현)
    @Query("""
        SELECT sg FROM StoreGoods sg
        JOIN FETCH sg.goods g
        JOIN FETCH g.animation a
        WHERE sg.store.id = :storeId
    """)
    List<StoreGoods> findByStoreId(@Param("storeId") Long storeId);

    // 상품 상세 조회 시, 이 상품을 파는 매장 목록
    List<StoreGoods> findByGoodsId(Long goodsId);

    // 관리자 권한 확인 및 goods 정보 fetch join
    @Query("""
        SELECT sg
        FROM StoreGoods sg
        JOIN FETCH sg.goods g WHERE sg.id = :storeGoodsId
    """)
    Optional<StoreGoods> findWithGoodsById(@Param("storeGoodsId") Long storeGoodsId);

    @Query("SELECT EXISTS(SELECT 1 FROM StoreGoods sg WHERE sg.store.id = :storeId AND sg.id = :storeGoodsId)")
    boolean existsByStoreIdAndStoreGoodsId(@Param("storeId") Long storeId, @Param("storeGoodsId") Long storeGoodsId);
}
