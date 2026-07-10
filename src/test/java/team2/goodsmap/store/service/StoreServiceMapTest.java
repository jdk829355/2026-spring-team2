package team2.goodsmap.store.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import team2.goodsmap.store.dto.request.CreateStoreRequest;
import team2.goodsmap.store.dto.response.StoreMapResponse;
import team2.goodsmap.store.enums.StoreType;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.enums.UserRole;
import team2.goodsmap.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
        createStoreAt("근처매장", BigDecimal.valueOf(37.5045), BigDecimal.valueOf(127.0));
        createStoreAt("중간매장", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.06));
        createStoreAt("먼매장", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.11));

        List<StoreMapResponse> result = storeService.getStoresForMap(37.5, 127.0, null, null, null);

        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).name()).isEqualTo("근처매장");
    }

    @Test
    void radius를_지정하면_해당_값으로_필터링한다() {
        createStoreAt("근처매장", BigDecimal.valueOf(37.5045), BigDecimal.valueOf(127.0));
        createStoreAt("중간매장", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.06));
        createStoreAt("먼매장", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.11));

        // 반경 6km -> 근처(500m), 중간(5.3km) 포함 / 먼(9.7km) 제외
        List<StoreMapResponse> result = storeService.getStoresForMap(37.5, 127.0, 6000.0, null, null);

        Assertions.assertThat(result).extracting(StoreMapResponse::name)
                .containsExactlyInAnyOrder("근처매장", "중간매장");
    }

    @Test
    void 결과는_거리_오름차순으로_정렬된다() {
        // 저장 순서를 일부러 뒤섞음 (결과가 저장 순서가 아니라 거리 순으로 나오는지 확인)
        createStoreAt("먼매장", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.11));
        createStoreAt("근처매장", BigDecimal.valueOf(37.5045), BigDecimal.valueOf(127.0));
        createStoreAt("중간매장", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.06));

        List<StoreMapResponse> result = storeService.getStoresForMap(37.5, 127.0, 15000.0, null, null);

        Assertions.assertThat(result).extracting(StoreMapResponse::name)
                .containsExactly("근처매장", "중간매장", "먼매장");
    }

    private void createStoreAt(String name, BigDecimal lat, BigDecimal lng) {
        CreateStoreRequest request = new CreateStoreRequest(
                name,
                "설명",
                StoreType.POPUP,
                LocalDate.now(),
                LocalDate.now().plusDays(3),
                "서울 강남구 테스트로 1",
                lat,
                lng
        );
        storeService.createStore(request, testUser.getId());
    }
}
