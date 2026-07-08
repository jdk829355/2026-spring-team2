package team2.goodsmap.store.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import team2.goodsmap.store.dto.request.CreateStoreRequest;
import team2.goodsmap.store.dto.response.StoreResponse;
import team2.goodsmap.store.enums.StoreType;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.enums.UserRole;
import team2.goodsmap.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@DataJpaTest
@Import(StoreService.class)
@ActiveProfiles("test")
class StoreServiceTest {

    @Autowired
    private StoreService storeService;
    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void each(){
        testUser = User.builder()
                .email("test@example.com")
                .password("password")
                .role(UserRole.STORE)
                .name("jung")
                .build();

        userRepository.save(testUser);
    }

    @Test
    void 업체_생성() {
        CreateStoreRequest req = createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));
        storeService.createStore(req, testUser.getId());
        List<StoreResponse> storeByUserId = storeService.getStoreByUserId(testUser.getId());

        Assertions.assertThat(storeByUserId).hasSize(1);
        Assertions.assertThat(storeByUserId.getFirst().name()).isEqualTo("애니메이트");
        Assertions.assertThat(storeByUserId.getFirst().description()).isEqualTo("다 있어요");
        Assertions.assertThat(storeByUserId.getFirst().type()).isEqualTo(StoreType.POPUP);
        Assertions.assertThat(storeByUserId.getFirst().startDate()).isEqualTo(LocalDate.of(2023, 1, 1));
        Assertions.assertThat(storeByUserId.getFirst().endDate()).isEqualTo(LocalDate.of(2023, 12, 31));
        Assertions.assertThat(storeByUserId.getFirst().address()).isEqualTo("서울특별시 마포구 양화로 188");
        Assertions.assertThat(storeByUserId.getFirst().lat()).isEqualByComparingTo(BigDecimal.valueOf(37.557743));
        Assertions.assertThat(storeByUserId.getFirst().lng()).isEqualByComparingTo(BigDecimal.valueOf(126.926487));
    }

    @Test
    void 관리_업체가_없으면_빈_배열을_반환한다() {
        List<StoreResponse> storeByUserId = storeService.getStoreByUserId(testUser.getId());

        Assertions.assertThat(storeByUserId).isEmpty();
    }

    @Test
    void 업체_생성_시작일이_종료일보다_늦으면_예외() {
        CreateStoreRequest req = createStoreRequest(LocalDate.of(2023, 12, 31), LocalDate.of(2023, 1, 1));

        Assertions.assertThatThrownBy(() -> storeService.createStore(req, testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작일은 종료일보다 늦을 수 없습니다.");
    }

    private CreateStoreRequest createStoreRequest(LocalDate startDate, LocalDate endDate) {
        return new CreateStoreRequest(
                "애니메이트",
                "다 있어요",
                StoreType.POPUP,
                startDate,
                endDate,
                "서울특별시 마포구 양화로 188",
                BigDecimal.valueOf(37.557743),
                BigDecimal.valueOf(126.926487)
        );
    }

}
