package team2.goodsmap.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team2.goodsmap.store.entity.StoreGoods;

import java.util.List;

public interface StoreGoodsRepository extends JpaRepository<StoreGoods, Long> {

    // 매장별 전체 재고 목록 조회
    List<StoreGoods> findByStoreId(Long storeId);

    // 상품 상세 조회 시, 이 상품을 파는 매장 목록
    List<StoreGoods> findByGoodsId(Long goodsId);
}
