package team2.goodsmap.store.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import team2.goodsmap.global.location.service.KakaoGeocodingService;
import team2.goodsmap.goods.entity.Goods;
import team2.goodsmap.goods.repository.GoodsRepository;
import team2.goodsmap.store.dto.request.AddExistingStoreGoodsRequest;
import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.entity.StoreGoods;
import team2.goodsmap.store.repository.StoreAdminRepository;
import team2.goodsmap.store.repository.StoreGoodsRepository;
import team2.goodsmap.store.repository.StoreRepository;
import team2.goodsmap.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class StoreServiceConstraintTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreAdminRepository storeAdminRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StoreGoodsRepository storeGoodsRepository;
    @Mock
    private GoodsRepository goodsRepository;
    @Mock
    private KakaoGeocodingService kakaoGeocodingService;
    @Mock
    private Store store;
    @Mock
    private Goods goods;

    @InjectMocks
    private StoreService storeService;

    @Test
    void 동시_등록으로_고유_제약조건이_위반되면_중복_오류로_변환한다() {
        prepareRegistration();
        willThrow(new DataIntegrityViolationException("constraint UQ_STORE_GOODS violated"))
                .given(storeGoodsRepository).saveAndFlush(any(StoreGoods.class));

        assertThatThrownBy(() -> storeService.createStoreGoods(request(), 1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 등록된 상품입니다.");
    }

    @Test
    void 다른_무결성_오류는_중복_오류로_변환하지_않는다() {
        prepareRegistration();
        DataIntegrityViolationException failure = new DataIntegrityViolationException("other constraint violated");
        willThrow(failure).given(storeGoodsRepository).saveAndFlush(any(StoreGoods.class));

        assertThatThrownBy(() -> storeService.createStoreGoods(request(), 1L, 1L))
                .isSameAs(failure);
    }

    private void prepareRegistration() {
        given(storeAdminRepository.existsByUserIdAndStoreId(1L, 1L)).willReturn(true);
        given(storeGoodsRepository.existsByStoreIdAndGoodsId(1L, 10L)).willReturn(false);
        given(goodsRepository.findById(10L)).willReturn(Optional.of(goods));
        given(storeRepository.findById(1L)).willReturn(Optional.of(store));
    }

    private AddExistingStoreGoodsRequest request() {
        return new AddExistingStoreGoodsRequest(10L, 5000, 10);
    }
}
