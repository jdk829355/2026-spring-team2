package team2.goodsmap.store.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import team2.goodsmap.global.location.dto.KakaoAddressSearchResponse;
import team2.goodsmap.global.location.service.KakaoGeocodingService;
import team2.goodsmap.store.dto.request.CreateStoreRequest;
import team2.goodsmap.store.dto.response.StoreMapResponse;
import team2.goodsmap.store.enums.StoreType;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.enums.UserRole;
import team2.goodsmap.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;

/**
 * StoreService.getStoresForMap (지도용 조회 - 반경 필터링 / 정렬) 전용 테스트.
 * StoreServiceTest와 동일한 @DataJpaTest 스타일 - 실제 저장된 Store로 검증한다.
 *
 * 중심 좌표 (37.5, 127.0) 기준 실측 거리:
 *   근처매장 (37.5045, 127.0)   ≈ 500m
 *   중간매장 (37.5,    127.06)  ≈ 5.3km
 *   먼매장   (37.5,    127.11)  ≈ 9.7km
 */
@DataJpaTest
@Import(StoreService.class)
@ActiveProfiles("test")
class StoreServiceMapTest {

    @Autowired
    private StoreService storeService;
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private KakaoGeocodingService kakaoGeocodingService;

    private User testUser;

    @BeforeEach
    void each() {
        testUser = User.builder()
                .email("map-test@example.com")
                .password("password")
                .role(UserRole.STORE)
                .name("map-tester")
                .build();

        userRepository.save(testUser);
    }

    @Test
    void 반경_미지정시_기본_3km_반경이_적용된다() {
        // geocoding mock: 각 주소별 위경도
        given(kakaoGeocodingService.searchAddress("경기도 용인시 기흥구 근처로 1"))
                .willReturn(doc("127.0", "37.5045"));
        given(kakaoGeocodingService.searchAddress("경기도 용인시 기흥구 중간로 2"))
                .willReturn(doc("127.06", "37.5"));
        given(kakaoGeocodingService.searchAddress("경기도 용인시 기흥구 먼로 3"))
                .willReturn(doc("127.11", "37.5"));

        createStoreAt("근처매장", "경기도 용인시 기흥구 근처로 1");
        createStoreAt("중간매장", "경기도 용인시 기흥구 중간로 2");
        createStoreAt("먼매장", "경기도 용인시 기흥구 먼로 3");

        List<StoreMapResponse> result = storeService.getStoresForMap(37.5, 127.0, null, null, null);

        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).name()).isEqualTo("근처매장");
    }

    @Test
    void radius를_지정하면_해당_값으로_필터링한다() {
        given(kakaoGeocodingService.searchAddress("경기도 용인시 기흥구 근처로 1"))
                .willReturn(doc("127.0", "37.5045"));
        given(kakaoGeocodingService.searchAddress("경기도 용인시 기흥구 중간로 2"))
                .willReturn(doc("127.06", "37.5"));
        given(kakaoGeocodingService.searchAddress("경기도 용인시 기흥구 먼로 3"))
                .willReturn(doc("127.11", "37.5"));

        createStoreAt("근처매장", "경기도 용인시 기흥구 근처로 1");
        createStoreAt("중간매장", "경기도 용인시 기흥구 중간로 2");
        createStoreAt("먼매장", "경기도 용인시 기흥구 먼로 3");

        // 반경 6km -> 근처(500m), 중간(5.3km) 포함 / 먼(9.7km) 제외
        List<StoreMapResponse> result = storeService.getStoresForMap(37.5, 127.0, 6000.0, null, null);

        Assertions.assertThat(result).extracting(StoreMapResponse::name)
                .containsExactlyInAnyOrder("근처매장", "중간매장");
    }

    @Test
    void 결과는_거리_오름차순으로_정렬된다() {
        given(kakaoGeocodingService.searchAddress("경기도 용인시 기흥구 먼로 3"))
                .willReturn(doc("127.11", "37.5"));
        given(kakaoGeocodingService.searchAddress("경기도 용인시 기흥구 근처로 1"))
                .willReturn(doc("127.0", "37.5045"));
        given(kakaoGeocodingService.searchAddress("경기도 용인시 기흥구 중간로 2"))
                .willReturn(doc("127.06", "37.5"));

        // 저장 순서를 일부러 뒤섞음 (결과가 저장 순서가 아니라 거리 순으로 나오는지 확인)
        createStoreAt("먼매장", "경기도 용인시 기흥구 먼로 3");
        createStoreAt("근처매장", "경기도 용인시 기흥구 근처로 1");
        createStoreAt("중간매장", "경기도 용인시 기흥구 중간로 2");

        List<StoreMapResponse> result = storeService.getStoresForMap(37.5, 127.0, 15000.0, null, null);

        Assertions.assertThat(result).extracting(StoreMapResponse::name)
                .containsExactly("근처매장", "중간매장", "먼매장");
    }

    private static KakaoAddressSearchResponse doc(String x, String y) {
        return new KakaoAddressSearchResponse(List.of(
                new KakaoAddressSearchResponse.Document("주소", x, y)
        ));
    }

    private void createStoreAt(String name, String address) {
        CreateStoreRequest request = new CreateStoreRequest(
                name,
                "설명",
                StoreType.POPUP,
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                address
        );
        storeService.createStore(request, testUser.getId());
    }
}
