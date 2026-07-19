package team2.goodsmap.goods.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.goods.dto.GoodsDetailResponse;
import team2.goodsmap.goods.dto.GoodsSearchRow;
import team2.goodsmap.goods.dto.GoodsSimpleResponse;
import team2.goodsmap.goods.entity.Animation;
import team2.goodsmap.goods.entity.Goods;
import team2.goodsmap.goods.repository.GoodsRepository;
import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.entity.StoreGoods;
import team2.goodsmap.store.enums.StoreType;
import team2.goodsmap.store.repository.StoreGoodsRepository;
import team2.goodsmap.support.EntityTestFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GoodsServiceTest {

    @Mock
    private GoodsRepository goodsRepository;

    @Mock
    private StoreGoodsRepository storeGoodsRepository;

    @InjectMocks
    private GoodsService goodsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(goodsService, "cdnUrl", "cdn.example.com");
    }

    @Test
    @DisplayName("q가 없으면 전체 상품을 조회한다 (등록용)")
    void getGoodsForRegistration_전체조회() {
        // given
        Animation animation = EntityTestFactory.animation(1L, "산리오 캐릭터즈");
        Goods goods = EntityTestFactory.goods(1L, "마이멜로디 텀블러", animation);
        given(goodsRepository.findAll()).willReturn(List.of(goods));

        // when
        List<GoodsSimpleResponse> result = goodsService.getGoodsForRegistration(null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAnimationTitle()).isEqualTo("산리오 캐릭터즈");
    }

    @Test
    @DisplayName("q가 있으면 이름으로 검색한다 (등록용)")
    void getGoodsForRegistration_검색() {
        // given
        Animation animation = EntityTestFactory.animation(1L, "산리오 캐릭터즈");
        Goods goods = EntityTestFactory.goods(1L, "마이멜로디 텀블러", animation);
        given(goodsRepository.findByNameContainingIgnoreCase("마이멜로디")).willReturn(List.of(goods));

        // when
        List<GoodsSimpleResponse> result = goodsService.getGoodsForRegistration("마이멜로디");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("마이멜로디 텀블러");
    }

    @Test
    @DisplayName("작품/지역/키워드로 상품과 해당 지역 업체 이미지들을 탐색한다")
    void searchGoods_필터조회() {
        // given
        given(goodsRepository.searchGoods(5L, "서울", "쿠로미")).willReturn(List.of(
                new GoodsSearchRow(12L, "쿠로미 인형", 5L, "산리오 캐릭터즈",
                        "stores/1/goods/12/images/57165dce-ae65-4da2-9d5b-69747ce06381.png"),
                new GoodsSearchRow(12L, "쿠로미 인형", 5L, "산리오 캐릭터즈",
                        "stores/2/goods/12/images/57165dce-ae65-4da2-9d5b-69747ce06382.png")
        ));

        // when
        List<GoodsSimpleResponse> result = goodsService.searchGoods(5L, "서울", "쿠로미");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAnimationId()).isEqualTo(5L);
        assertThat(result.get(0).getImageUrls()).containsExactly(
                "https://cdn.example.com/stores/1/goods/12/images/57165dce-ae65-4da2-9d5b-69747ce06381.png",
                "https://cdn.example.com/stores/2/goods/12/images/57165dce-ae65-4da2-9d5b-69747ce06382.png"
        );
    }

    @Test
    @DisplayName("이미지가 없는 상품은 빈 이미지 목록을 반환한다")
    void searchGoods_이미지없음() {
        given(goodsRepository.searchGoods(null, null, null)).willReturn(List.of(
                new GoodsSearchRow(12L, "쿠로미 인형", 5L, "산리오 캐릭터즈", null)
        ));

        List<GoodsSimpleResponse> result = goodsService.searchGoods(null, null, null);

        assertThat(result.getFirst().getImageUrls()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 상품을 상세 조회하면 NotFoundException이 발생한다")
    void getGoodsDetail_존재하지않으면_예외() {
        // given
        given(goodsRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> goodsService.getGoodsDetail(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("상품 상세 조회 시 판매 중인 매장 목록을 함께 반환한다")
    void getGoodsDetail_매장목록포함() {
        // given
        Animation animation = EntityTestFactory.animation(5L, "산리오 캐릭터즈");
        Goods goods = EntityTestFactory.goods(12L, "마이멜로디 텀블러", animation);
        Store store = EntityTestFactory.store(7L, "강남점", StoreType.POPUP, "서울특별시 강남구",
                BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0));
        StoreGoods storeGoods = EntityTestFactory.storeGoods(
                58L,
                15000,
                30,
                "stores/7/goods/58/images/57165dce-ae65-4da2-9d5b-69747ce06381.png",
                goods,
                store
        );

        given(goodsRepository.findById(12L)).willReturn(Optional.of(goods));
        given(storeGoodsRepository.findByGoodsId(12L)).willReturn(List.of(storeGoods));

        // when
        GoodsDetailResponse result = goodsService.getGoodsDetail(12L);

        // then
        assertThat(result.getName()).isEqualTo("마이멜로디 텀블러");
        assertThat(result.getStores()).hasSize(1);
        assertThat(result.getStores().get(0).getStoreName()).isEqualTo("강남점");
        assertThat(result.getStores().get(0).getPrice()).isEqualTo(15000);
        assertThat(result.getStores().get(0).getImagePath()).isEqualTo(
                "https://cdn.example.com/stores/7/goods/58/images/57165dce-ae65-4da2-9d5b-69747ce06381.png"
        );
    }

    @Test
    @DisplayName("상품을 판매하는 매장이 없으면 빈 배열을 반환한다")
    void getGoodsDetail_매장없으면_빈배열() {
        // given
        Animation animation = EntityTestFactory.animation(5L, "산리오 캐릭터즈");
        Goods goods = EntityTestFactory.goods(12L, "마이멜로디 텀블러", animation);

        given(goodsRepository.findById(12L)).willReturn(Optional.of(goods));
        given(storeGoodsRepository.findByGoodsId(12L)).willReturn(List.of());

        // when
        GoodsDetailResponse result = goodsService.getGoodsDetail(12L);

        // then
        assertThat(result.getStores()).isEmpty();
    }
}
