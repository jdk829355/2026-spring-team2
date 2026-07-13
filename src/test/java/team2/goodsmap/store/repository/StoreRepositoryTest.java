package team2.goodsmap.store.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import team2.goodsmap.global.config.JpaAuditingConfig;
import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.entity.StoreAdmin;
import team2.goodsmap.store.enums.StoreType;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.enums.UserRole;
import team2.goodsmap.user.repository.UserRepository;

import java.math.BigDecimal;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class StoreRepositoryTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreAdminRepository storeAdminRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void store_저장시_createdAt_자동_설정() {
        Store store = Store.builder()
                .name("테스트매장")
                .description("설명")
                .type(StoreType.POPUP)
                .address("서울시 강남구")
                .lat(BigDecimal.valueOf(37.5))
                .lng(BigDecimal.valueOf(127.0))
                .build();

        storeRepository.save(store);

        Assertions.assertThat(store.getCreatedAt()).isNotNull();
    }

    @Test
    void storeAdmin_저장시_createdAt_자동_설정() {
        User user = userRepository.save(User.builder()
                .name("tester")
                .email("tester@example.com")
                .password("pw")
                .role(UserRole.STORE)
                .build());

        Store store = storeRepository.save(Store.builder()
                .name("매장")
                .type(StoreType.POPUP)
                .address("서울시")
                .lat(BigDecimal.valueOf(37.5))
                .lng(BigDecimal.valueOf(127.0))
                .build());

        StoreAdmin admin = StoreAdmin.builder()
                .user(user)
                .store(store)
                .build();

        storeAdminRepository.save(admin);

        Assertions.assertThat(admin.getCreatedAt()).isNotNull();
    }
}
