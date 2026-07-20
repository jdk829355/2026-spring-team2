package team2.goodsmap.goods.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import team2.goodsmap.goods.dto.GoodsSearchRow;
import team2.goodsmap.goods.entity.Animation;
import team2.goodsmap.goods.entity.Goods;
import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.entity.StoreGoods;
import team2.goodsmap.store.enums.StoreType;
import team2.goodsmap.store.repository.StoreGoodsRepository;
import team2.goodsmap.store.repository.StoreRepository;
import team2.goodsmap.support.EntityTestFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class GoodsRepositoryTest {

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private AnimationRepository animationRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreGoodsRepository storeGoodsRepository;

    @Test
    void 등록용_상품_검색은_첫_이미지만_조회한다() {
        Animation animation = animationRepository.save(EntityTestFactory.animation(null, "귀멸의 칼날"));
        Goods goods = goodsRepository.save(Goods.builder()
                .name("탄지로 아크릴 스탠드")
                .animation(animation)
                .build());
        Store seoulStore = storeRepository.save(store("서울특별시 마포구"));
        Store busanStore = storeRepository.save(store("부산광역시 해운대구"));
        Store incheonStore = storeRepository.save(store("인천광역시 연수구"));

        storeGoodsRepository.save(storeGoods(goods, seoulStore, null));
        storeGoodsRepository.save(storeGoods(goods, busanStore, "stores/1/goods/1/images/first.png"));
        storeGoodsRepository.save(storeGoods(goods, incheonStore, "stores/2/goods/1/images/second.png"));

        List<GoodsSearchRow> result = goodsRepository.findGoodsForRegistration("탄지로");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().imagePath()).isEqualTo("stores/1/goods/1/images/first.png");
    }

    @Test
    void 지역에_해당하는_업체의_이미지만_조회한다() {
        Animation animation = animationRepository.save(EntityTestFactory.animation(null, "귀멸의 칼날"));
        Goods goodsWithImages = goodsRepository.save(Goods.builder()
                .name("탄지로 아크릴 스탠드")
                .animation(animation)
                .build());
        Goods goodsWithoutImage = goodsRepository.save(Goods.builder()
                .name("네즈코 키링")
                .animation(animation)
                .build());

        Store seoulStore = storeRepository.save(store("서울특별시 마포구"));
        Store busanStore = storeRepository.save(store("부산광역시 해운대구"));

        storeGoodsRepository.save(storeGoods(
                goodsWithImages,
                seoulStore,
                "stores/1/goods/1/images/seoul.png"
        ));
        storeGoodsRepository.save(storeGoods(
                goodsWithImages,
                busanStore,
                "stores/2/goods/1/images/busan.png"
        ));
        storeGoodsRepository.save(storeGoods(goodsWithoutImage, seoulStore, null));

        List<GoodsSearchRow> result = goodsRepository.searchGoods(null, "서울", null);

        assertThat(result).extracting(GoodsSearchRow::imagePath)
                .containsExactlyInAnyOrder("stores/1/goods/1/images/seoul.png", null);
        assertThat(result).extracting(GoodsSearchRow::imagePath)
                .doesNotContain("stores/2/goods/1/images/busan.png");
    }

    private Store store(String address) {
        return Store.builder()
                .name(address)
                .description("테스트 업체")
                .type(StoreType.STORE)
                .address(address)
                .lat(BigDecimal.valueOf(37.5))
                .lng(BigDecimal.valueOf(127.0))
                .build();
    }

    private StoreGoods storeGoods(Goods goods, Store store, String imagePath) {
        return StoreGoods.builder()
                .price(10_000)
                .stock(10)
                .imagePath(imagePath)
                .goods(goods)
                .store(store)
                .build();
    }
}
