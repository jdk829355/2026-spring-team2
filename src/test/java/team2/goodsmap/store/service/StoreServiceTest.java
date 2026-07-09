package team2.goodsmap.store.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import team2.goodsmap.store.dto.request.AddStoreAdminRequest;
import team2.goodsmap.store.dto.request.CreateStoreRequest;
import team2.goodsmap.store.dto.response.StoreAdminResponse;
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
    void 시작일이_종료일보다_늦으면_예외가_발생한다() {
        CreateStoreRequest req = createStoreRequest(LocalDate.of(2023, 12, 31), LocalDate.of(2023, 1, 1));

        Assertions.assertThatThrownBy(() -> storeService.createStore(req, testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작일은 종료일보다 늦을 수 없습니다.");
    }

    @Test
    void 업체에_관리자_추가_및_조회(){
        User testUser2 = User.builder()
                .email("test2@example.com")
                .password("password")
                .role(UserRole.USER)
                .name("jung2")
                .build();

        userRepository.save(testUser2);

        StoreResponse storeResponse = storeService.createStore(createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)), testUser.getId());

        // 관리자 추가
        StoreAdminResponse response = storeService.createStoreAdmin(new AddStoreAdminRequest(testUser2.getEmail()), storeResponse.id());

        // 관리자 조회
        // 관리자가 두 명인지, 그 두 명이 testUser, testUser2인지 확인
        List<StoreAdminResponse> admins = storeService.getStoreAdmin(storeResponse.id());
        Assertions.assertThat(admins).hasSize(2);
        Assertions.assertThat(admins).extracting(StoreAdminResponse::userId).containsExactlyInAnyOrder(testUser.getId(), testUser2.getId());
    }

    @Test
    void 관리자_삭제_성공() {
        // given: USER role 관리자 추가
        User userAdmin = User.builder()
                .email("admin@example.com")
                .password("password")
                .role(UserRole.USER)
                .name("admin-user")
                .build();
        userRepository.save(userAdmin);

        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        StoreAdminResponse addedAdmin = storeService.createStoreAdmin(
                new AddStoreAdminRequest(userAdmin.getEmail()), store.id());

        // when: STORE role 소유자가 USER role 관리자 삭제
        storeService.deleteStoreAdmin(testUser.getId(), store.id(), addedAdmin.id());

        // then: 관리자 목록에서 삭제된 관리자가 제외됨
        List<StoreAdminResponse> admins = storeService.getStoreAdmin(store.id());
        Assertions.assertThat(admins).hasSize(1);
        Assertions.assertThat(admins.getFirst().userId()).isEqualTo(testUser.getId());
    }

    @Test
    void 관리자_삭제_요청자_STORE_role_아니면_예외() {
        // given
        User userRoleUser = User.builder()
                .email("user@example.com")
                .password("password")
                .role(UserRole.USER)
                .name("regular-user")
                .build();
        userRepository.save(userRoleUser);

        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        // when & then: USER role 사용자가 삭제 시도하면 예외
        Assertions.assertThatThrownBy(() ->
                        storeService.deleteStoreAdmin(userRoleUser.getId(), store.id(), 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("관리 권한이 없습니다.");
    }

    @Test
    void 관리자_삭제_대상이_존재하지_않으면_예외() {
        // given
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        // when & then: 존재하지 않는 storeAdminId로 삭제 시도
        Assertions.assertThatThrownBy(() ->
                        storeService.deleteStoreAdmin(testUser.getId(), store.id(), 9999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당하는 관리자가 없습니다.");
    }

    @Test
    void 관리자_삭제_대상이_USER_role_아니면_예외() {
        // given: STORE role 사용자를 추가 관리자로 등록
        User anotherStoreOwner = User.builder()
                .email("another-store@example.com")
                .password("password")
                .role(UserRole.STORE)
                .name("another-store-owner")
                .build();
        userRepository.save(anotherStoreOwner);

        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        StoreAdminResponse storeRoleAdmin = storeService.createStoreAdmin(
                new AddStoreAdminRequest(anotherStoreOwner.getEmail()), store.id());

        // when & then: STORE role 관리자는 삭제 대상이 아니므로 예외
        Assertions.assertThatThrownBy(() ->
                        storeService.deleteStoreAdmin(testUser.getId(), store.id(), storeRoleAdmin.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 사용자는 삭제 대상이 아닙니다.");
    }

    @Test
    void 관리자_삭제_요청자가_해당_업체_관리자_아니면_예외() {
        // given: 별도 STORE role 사용자 생성 (다른 store의 owner)
        User otherStoreOwner = User.builder()
                .email("other-owner@example.com")
                .password("password")
                .role(UserRole.STORE)
                .name("other-owner")
                .build();
        userRepository.save(otherStoreOwner);

        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        // when & then: 다른 store의 소유자가 삭제 시도하면 예외
        Assertions.assertThatThrownBy(() ->
                        storeService.deleteStoreAdmin(otherStoreOwner.getId(), store.id(), 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 업체의 관리 권한이 없습니다.");
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
