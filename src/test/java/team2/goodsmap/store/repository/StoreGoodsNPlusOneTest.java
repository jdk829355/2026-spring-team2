package team2.goodsmap.store.repository;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import team2.goodsmap.goods.entity.Animation;
import team2.goodsmap.goods.entity.Goods;
import team2.goodsmap.goods.repository.AnimationRepository;
import team2.goodsmap.goods.repository.GoodsRepository;
import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.entity.StoreGoods;
import team2.goodsmap.store.enums.StoreType;
import team2.goodsmap.support.EntityTestFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상품 상세의 "이 상품을 파는 매장 목록"(findByGoodsId)에서 N+1이 생기지 않는지 확인한다.
 * 매장 수(N)를 바꿔도 조회 + 매장 이름 접근에 드는 쿼리 수가 1번으로 고정되어야 한다.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
class StoreGoodsNPlusOneTest {

    @Autowired private AnimationRepository animationRepository;
    @Autowired private GoodsRepository goodsRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private StoreGoodsRepository storeGoodsRepository;
    @Autowired private EntityManager em;

    @ParameterizedTest(name = "매장 {0}곳")
    @ValueSource(ints = {5, 20})
    void 상품_상세의_매장_목록은_매장_수와_관계없이_쿼리_1번(int storeCount) {
        Animation animation = animationRepository.save(EntityTestFactory.animation(null, "귀멸의 칼날"));
        Goods goods = goodsRepository.save(Goods.builder().name("탄지로 아크릴 스탠드").animation(animation).build());
        for (int i = 0; i < storeCount; i++) {
            Store store = storeRepository.save(Store.builder()
                    .name("매장" + i).description("테스트 업체").type(StoreType.STORE)
                    .address("서울특별시 마포구 " + i)
                    .lat(BigDecimal.valueOf(37.5)).lng(BigDecimal.valueOf(127.0))
                    .build());
            storeGoodsRepository.save(StoreGoods.builder()
                    .price(10_000).stock(10).goods(goods).store(store).build());
        }
        em.flush();
        em.clear();   // 1차 캐시를 비워 실제 조회 쿼리만 센다

        Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        List<StoreGoods> rows = storeGoodsRepository.findByGoodsId(goods.getId());
        rows.forEach(sg -> sg.getStore().getName());   // GoodsService.getGoodsDetail 처럼 매장 정보 접근

        long queries = stats.getPrepareStatementCount();
        System.out.printf("[N+1 측정] 매장 %d곳 → 쿼리 %d번%n", storeCount, queries);

        assertThat(rows).hasSize(storeCount);
        assertThat(queries).isEqualTo(1);
    }
}
