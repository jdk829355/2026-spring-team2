package team2.goodsmap.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team2.goodsmap.store.entity.StoreGoods;

import java.util.List;

public interface StoreGoodsRepository extends JpaRepository<StoreGoods, Long> {

    // 매장별 전체 재고 목록 조회
    // TODO: N+1 문제 있음. storeGoods 개수가 많아지면 goods/animation lazy loading 때문에 쿼리가 N번 더 나감.
// 나중에 findByStoreId 쿼리를 JOIN FETCH로 바꿀 것.
    List<StoreGoods> findByStoreId(Long storeId);

    // 상품 상세 조회 시, 이 상품을 파는 매장 목록
    List<StoreGoods> findByGoodsId(Long goodsId);
}
