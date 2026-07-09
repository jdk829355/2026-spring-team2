package team2.goodsmap.store.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.goods.entity.Animation;
import team2.goodsmap.goods.entity.Goods;
import team2.goodsmap.store.dto.StoreGoodsItemResponse;
import team2.goodsmap.store.dto.StoreMapResponse;
import team2.goodsmap.store.dto.StoreResponse;
import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.entity.StoreGoods;
import team2.goodsmap.store.enums.StoreType;
import team2.goodsmap.store.repository.StoreGoodsRepository;
import team2.goodsmap.store.repository.StoreRepository;
import team2.goodsmap.support.EntityTestFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreGoodsRepository storeGoodsRepository;

    @InjectMocks
    private StoreService storeService;

    @Test
    @DisplayName("작품/지역/키워드로 스토어 목록을 조회한다")
    void getStores_필터조회() {
        // given
        Store store = EntityTestFactory.store(7L, "강남점", StoreType.POPUP, "서울특별시 강남구",
                BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0));
        given(storeRepository.searchStores(5L, "서울", null)).willReturn(List.of(store));

        // when
        List<StoreResponse> result = storeService.getStores(5L, "서울", null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("강남점");
        assertThat(result.get(0).getType()).isEqualTo("POPUP");
    }

    @Test
    @DisplayName("지도 조회 시 반경 밖의 매장은 결과에서 제외된다")
    void getStoresForMap_반경필터() {
        // given: 서울시청 좌표 기준으로 근처(강남)와 먼 곳(부산)
        Store near = EntityTestFactory.store(1L, "근처매장", StoreType.POPUP, "서울특별시",
                BigDecimal.valueOf(37.5665), BigDecimal.valueOf(126.9780));
        Store far = EntityTestFactory.store(2L, "먼매장", StoreType.STORE, "부산광역시",
                BigDecimal.valueOf(35.1796), BigDecimal.valueOf(129.0756));

        given(storeRepository.findAllForMap(null, null)).willReturn(List.of(near, far));

        // when: 반경 5km로 조회
        List<StoreMapResponse> result = storeService.getStoresForMap(
                37.5665, 126.9780, 5000.0, null, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("근처매장");
    }

    @Test
    @DisplayName("지도 조회 시 결과는 거리순으로 정렬된다")
    void getStoresForMap_거리순정렬() {
        // given
        Store far = EntityTestFactory.store(1L, "먼곳", StoreType.POPUP, "서울특별시",
                BigDecimal.valueOf(37.6000), BigDecimal.valueOf(127.0000));
        Store near = EntityTestFactory.store(2L, "가까운곳", StoreType.POPUP, "서울특별시",
                BigDecimal.valueOf(37.5665), BigDecimal.valueOf(126.9780));

        given(storeRepository.findAllForMap(null, null)).willReturn(List.of(far, near));

        // when
        List<StoreMapResponse> result = storeService.getStoresForMap(
                37.5665, 126.9780, 50000.0, null, null);

        // then
        assertThat(result).extracting(StoreMapResponse::getName)
                .containsExactly("가까운곳", "먼곳");
    }

    @Test
    @DisplayName("radius를 지정하지 않으면 기본 반경(3km)으로 필터링된다")
    void getStoresForMap_기본반경() {
        // given
        Store within = EntityTestFactory.store(1L, "3km이내", StoreType.POPUP, "서울특별시",
                BigDecimal.valueOf(37.5665), BigDecimal.valueOf(126.9780));

        given(storeRepository.findAllForMap(null, null)).willReturn(List.of(within));

        // when: radius = null
        List<StoreMapResponse> result = storeService.getStoresForMap(
                37.5665, 126.9780, null, null, null);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 스토어의 재고를 조회하면 NotFoundException이 발생한다")
    void getStoreGoods_존재하지않으면_예외() {
        // given
        given(storeRepository.existsById(999L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> storeService.getStoreGoods(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("매장별 전체 재고 목록을 조회한다")
    void getStoreGoods_성공() {
        // given
        Animation animation = EntityTestFactory.animation(5L, "산리오 캐릭터즈");
        Goods goods = EntityTestFactory.goods(12L, "마이멜로디 텀블러", animation);
        Store store = EntityTestFactory.store(7L, "강남점", StoreType.POPUP, "서울특별시",
                BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0));
        StoreGoods storeGoods = EntityTestFactory.storeGoods(58L, 15000, 30, "img.png", goods, store);

        given(storeRepository.existsById(7L)).willReturn(true);
        given(storeGoodsRepository.findByStoreId(7L)).willReturn(List.of(storeGoods));

        // when
        List<StoreGoodsItemResponse> result = storeService.getStoreGoods(7L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGoodsName()).isEqualTo("마이멜로디 텀블러");
        assertThat(result.get(0).getPrice()).isEqualTo(15000);
    }
}
