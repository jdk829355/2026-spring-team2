package team2.goodsmap.store.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import team2.goodsmap.goods.dto.GoodsResponse;
import team2.goodsmap.goods.entity.Animation;
import team2.goodsmap.goods.entity.Goods;
import team2.goodsmap.goods.repository.AnimationRepository;
import team2.goodsmap.goods.repository.GoodsRepository;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.global.location.dto.KakaoAddressSearchResponse;
import team2.goodsmap.global.location.service.KakaoGeocodingService;
import team2.goodsmap.store.dto.request.*;
import team2.goodsmap.store.dto.response.StoreAdminResponse;
import team2.goodsmap.store.dto.response.StoreDetailResponse;
import team2.goodsmap.store.dto.response.StoreGoodsResponse;
import team2.goodsmap.store.dto.response.StoreResponse;
import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.enums.StoreType;
import team2.goodsmap.store.repository.StoreGoodsRepository;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.enums.UserRole;
import team2.goodsmap.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@DataJpaTest
@Import(StoreService.class)
@ActiveProfiles("test")
class StoreServiceTest {

    @Autowired
    private StoreService storeService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AnimationRepository animationRepository;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private StoreGoodsRepository storeGoodsRepository;

    @MockitoBean
    private KakaoGeocodingService kakaoGeocodingService;

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

        // geocoding mock: 도로명 주소 → 위경도 응답
        given(kakaoGeocodingService.searchAddress(anyString()))
                .willReturn(new KakaoAddressSearchResponse(List.of(
                        new KakaoAddressSearchResponse.Document(
                                "서울특별시 마포구 양화로 188",
                                "126.926487",
                                "37.557743"
                        )
                )));
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
        StoreAdminResponse response = storeService.createStoreAdmin(new AddStoreAdminRequest(testUser2.getEmail()), storeResponse.id(), testUser.getId());

        // 관리자 조회
        // 관리자가 두 명인지, 그 두 명이 testUser, testUser2인지 확인
        List<StoreAdminResponse> admins = storeService.getStoreAdmin(storeResponse.id(), testUser.getId());
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
                new AddStoreAdminRequest(userAdmin.getEmail()), store.id(), testUser.getId());

        // when: STORE role 소유자가 USER role 관리자 삭제
        storeService.deleteStoreAdmin(testUser.getId(), store.id(), addedAdmin.id());

        // then: 관리자 목록에서 삭제된 관리자가 제외됨
        List<StoreAdminResponse> admins = storeService.getStoreAdmin(store.id(), testUser.getId());
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
                new AddStoreAdminRequest(anotherStoreOwner.getEmail()), store.id(), testUser.getId());

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

    @Test
    void 새상품으로_storeGoods를_생성한다() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());
        Animation animation = animationRepository.save(animation("하이큐"));
        Goods goods = goodsRepository.save(Goods.builder()
                .name("아크릴 스탠드")
                .animation(animation)
                .build());

        AddNewStoreGoodsRequest request = new AddNewStoreGoodsRequest(
                null,
                15000,
                30
        );
        GoodsResponse goodsResponse = new GoodsResponse(
                goods.getId(),
                goods.getName(),
                animation.getId(),
                animation.getTitle()
        );

        StoreGoodsResponse response = storeService.createStoreGoods(request, goodsResponse, testUser.getId(), store.id());

        Assertions.assertThat(response.storeId()).isEqualTo(store.id());
        Assertions.assertThat(response.goodsInfo().id()).isEqualTo(goods.getId());
        Assertions.assertThat(response.price()).isEqualTo(15000);
        Assertions.assertThat(response.stock()).isEqualTo(30);
        Assertions.assertThat(response.imagePath()).isNull();
        Assertions.assertThat(storeGoodsRepository.findByStoreId(store.id())).hasSize(1);
    }

    @Test
    void 기존상품으로_storeGoods를_생성한다() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());
        Animation animation = animationRepository.save(animation("슬램덩크"));
        Goods goods = goodsRepository.save(Goods.builder()
                .name("포토카드")
                .animation(animation)
                .build());

        AddExistingStoreGoodsRequest request = new AddExistingStoreGoodsRequest(
                goods.getId(),
                5000,
                12
        );

        StoreGoodsResponse response = storeService.createStoreGoods(request, testUser.getId(), store.id());

        Assertions.assertThat(response.storeId()).isEqualTo(store.id());
        Assertions.assertThat(response.goodsInfo().id()).isEqualTo(goods.getId());
        Assertions.assertThat(response.price()).isEqualTo(5000);
        Assertions.assertThat(response.stock()).isEqualTo(12);
        Assertions.assertThat(storeGoodsRepository.findByStoreId(store.id())).hasSize(1);
    }

    @Test
    void 이미_등록된_기존상품은_storeGoods로_중복_생성할_수_없다() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());
        Animation animation = animationRepository.save(animation("슬램덩크"));
        Goods goods = goodsRepository.save(Goods.builder()
                .name("포토카드")
                .animation(animation)
                .build());
        AddExistingStoreGoodsRequest request = new AddExistingStoreGoodsRequest(goods.getId(), 5000, 12);

        storeService.createStoreGoods(request, testUser.getId(), store.id());

        Assertions.assertThatThrownBy(() -> storeService.createStoreGoods(request, testUser.getId(), store.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 등록된 상품입니다.");
        Assertions.assertThat(storeGoodsRepository.findByStoreId(store.id())).hasSize(1);
    }

    @Test
    void storeGoods_삭제_성공() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());
        Animation animation = animationRepository.save(animation("슬램덩크"));
        Goods goods = goodsRepository.save(Goods.builder()
                .name("포토카드")
                .animation(animation)
                .build());

        AddExistingStoreGoodsRequest request = new AddExistingStoreGoodsRequest(
                goods.getId(),
                5000,
                12
        );
        StoreGoodsResponse storeGoods = storeService.createStoreGoods(request, testUser.getId(), store.id());

        storeService.deleteStoreGoods(store.id(), storeGoods.id(), testUser.getId());

        Assertions.assertThat(storeGoodsRepository.findByStoreId(store.id())).isEmpty();
    }

    @Test
    void storeGoods_삭제_권한_없음() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        User otherUser = User.builder()
                .email("other@example.com")
                .password("password")
                .role(UserRole.USER)
                .name("other")
                .build();
        userRepository.save(otherUser);

        Assertions.assertThatThrownBy(() ->
                        storeService.deleteStoreGoods(store.id(), 1L, otherUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("삭제 권한이 없습니다.");
    }

    @Test
    void storeGoods_삭제_상품_없음() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        Assertions.assertThatThrownBy(() ->
                        storeService.deleteStoreGoods(store.id(), 9999L, testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 상품이 없습니다.");
    }

    @Test
    void 가격과_재고를_수정한다(){
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        Animation animation = animationRepository.save(animation("슬램덩크"));

        Goods goods = goodsRepository.save(Goods.builder()
                .name("포토카드")
                .animation(animation)
                .build());

        AddExistingStoreGoodsRequest request = new AddExistingStoreGoodsRequest(
                goods.getId(),
                5000,
                12
        );

        StoreGoodsResponse storeGoods = storeService.createStoreGoods(request, testUser.getId(), store.id());

        String imagePath = "stores/" + store.id() + "/goods/" + storeGoods.id()
                + "/images/57165dce-ae65-4da2-9d5b-69747ce06381.png";
        StoreGoodsResponse storeGoodsResponse = storeService.modifyStoreGoods(
                store.id(),
                storeGoods.id(),
                new UpdateStoreGoodsRequest(6000, 15, imagePath),
                testUser.getId()
        );
        Assertions.assertThat(storeGoodsResponse.price()).isEqualTo(6000);
        Assertions.assertThat(storeGoodsResponse.stock()).isEqualTo(15);
        Assertions.assertThat(storeGoodsResponse.imagePath()).isEqualTo("https://cdn.example.com/" + imagePath);
        Assertions.assertThat(storeGoodsRepository.findById(storeGoods.id()).orElseThrow().getImagePath())
                .isEqualTo(imagePath);
    }

    @Test
    void 업체_정보를_수정한다() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        UpdateStoreRequest request = new UpdateStoreRequest(
                "수정된 이름", "수정된 설명", StoreType.POPUP,
                LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30),
                "변경된 주소"
        );

        StoreResponse updated = storeService.updateStore(request, store.id(), testUser.getId());

        Assertions.assertThat(updated.name()).isEqualTo("수정된 이름");
        Assertions.assertThat(updated.description()).isEqualTo("수정된 설명");
        Assertions.assertThat(updated.startDate()).isEqualTo(LocalDate.of(2025, 6, 1));
        Assertions.assertThat(updated.endDate()).isEqualTo(LocalDate.of(2025, 6, 30));
        Assertions.assertThat(updated.address()).isEqualTo("변경된 주소");
        Assertions.assertThat(updated.lat()).isEqualByComparingTo(BigDecimal.valueOf(37.557743));
        Assertions.assertThat(updated.lng()).isEqualByComparingTo(BigDecimal.valueOf(126.926487));
    }

    @Test
    void 업체_수정_권한_없음() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        User otherUser = User.builder()
                .email("other@example.com")
                .password("password")
                .role(UserRole.USER)
                .name("other")
                .build();
        userRepository.save(otherUser);

        UpdateStoreRequest request = new UpdateStoreRequest(
                "수정된 이름", null, null, null, null, null
        );

        Assertions.assertThatThrownBy(() ->
                        storeService.updateStore(request, store.id(), otherUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수정 권한이 없습니다.");
    }

    @Test
    void updateStore_시작일이_종료일보다_늦으면_예외가_발생한다() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        UpdateStoreRequest request = new UpdateStoreRequest(
                null, null, null,
                LocalDate.of(2025, 12, 31), LocalDate.of(2025, 1, 1),
                null
        );

        Assertions.assertThatThrownBy(() ->
                        storeService.updateStore(request, store.id(), testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작일은 종료일보다 늦을 수 없습니다.");
    }

    @Test
    void updateStore_시작일만_갱신할_때_기존_종료일보다_늦으면_예외가_발생한다() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        UpdateStoreRequest request = new UpdateStoreRequest(
                null, null, null,
                LocalDate.of(2024, 1, 1), null,  // 기존 endDate(2023-12-31)보다 늦음
                null
        );

        Assertions.assertThatThrownBy(() ->
                        storeService.updateStore(request, store.id(), testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작일은 종료일보다 늦을 수 없습니다.");
    }

    @Test
    void updateStore_종료일만_갱신할_때_기존_시작일보다_빠르면_예외가_발생한다() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 6, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        UpdateStoreRequest request = new UpdateStoreRequest(
                null, null, null,
                null, LocalDate.of(2023, 5, 31),  // 기존 startDate(2023-06-01)보다 빠름
                null
        );

        Assertions.assertThatThrownBy(() ->
                        storeService.updateStore(request, store.id(), testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작일은 종료일보다 늦을 수 없습니다.");
    }

    @Test
    void getStoreDetail_스토어_상세_조회_성공() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        StoreDetailResponse detail = storeService.getStoreDetail(store.id(), testUser.getId());

        Assertions.assertThat(detail.id()).isEqualTo(store.id());
        Assertions.assertThat(detail.name()).isEqualTo("애니메이트");
        Assertions.assertThat(detail.description()).isEqualTo("다 있어요");
        Assertions.assertThat(detail.type()).isEqualTo(StoreType.POPUP);
        Assertions.assertThat(detail.startDate()).isEqualTo(LocalDate.of(2023, 1, 1));
        Assertions.assertThat(detail.endDate()).isEqualTo(LocalDate.of(2023, 12, 31));
        Assertions.assertThat(detail.address()).isEqualTo("서울특별시 마포구 양화로 188");
        Assertions.assertThat(detail.lat()).isEqualByComparingTo(BigDecimal.valueOf(37.557743));
        Assertions.assertThat(detail.lng()).isEqualByComparingTo(BigDecimal.valueOf(126.926487));
        Assertions.assertThat(detail.goodsCount()).isEqualTo(0L);
    }

    @Test
    void updateStore_기존_startDate가_null일_때_endDate만_갱신한다() {
        // Store를 startDate, endDate 없이 생성
        StoreResponse store = storeService.createStore(
                createStoreRequest(null, null),
                testUser.getId());

        Assertions.assertThat(store.startDate()).isNull();
        Assertions.assertThat(store.endDate()).isNull();

        UpdateStoreRequest request = new UpdateStoreRequest(
                null, null, null,
                null, LocalDate.of(2025, 12, 31),
                null
        );

        StoreResponse updated = storeService.updateStore(request, store.id(), testUser.getId());

        Assertions.assertThat(updated.startDate()).isNull();
        Assertions.assertThat(updated.endDate()).isEqualTo(LocalDate.of(2025, 12, 31));
    }

    @Test
    void getStoreDetail_스토어가_존재하지_않으면_예외() {
        Assertions.assertThatThrownBy(() -> storeService.getStoreDetail(9999L, testUser.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 스토어입니다.");
    }

    @Test
    void storeGoods_이미지_경로_수정_성공() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());
        Animation animation = animationRepository.save(animation("슬램덩크"));
        Goods goods = goodsRepository.save(Goods.builder()
                .name("포토카드")
                .animation(animation)
                .build());

        AddExistingStoreGoodsRequest addRequest = new AddExistingStoreGoodsRequest(
                goods.getId(), 5000, 12);
        StoreGoodsResponse storeGoods = storeService.createStoreGoods(addRequest, testUser.getId(), store.id());

        AddImagePathRequest imagePathRequest = new AddImagePathRequest(
                "stores/1/goods/" + storeGoods.id() + "/images/550e8400-e29b-41d4-a716-446655440000.png");
        storeService.updateImagePath(testUser.getId(), store.id(), storeGoods.id(), imagePathRequest);

        // DB에서 재조회하여 확인
        var updated = storeGoodsRepository.findById(storeGoods.id()).orElseThrow();
        Assertions.assertThat(updated.getImagePath())
                .isEqualTo("stores/1/goods/" + storeGoods.id() + "/images/550e8400-e29b-41d4-a716-446655440000.png");

        Assertions.assertThat(storeService.getStoreGoods(store.id()).getFirst().imagePath())
                .isEqualTo("https://cdn.example.com/stores/1/goods/" + storeGoods.id()
                        + "/images/550e8400-e29b-41d4-a716-446655440000.png");
    }

    @Test
    void storeGoods_이미지_경로_수정_권한_없음() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        User otherUser = User.builder()
                .email("other@example.com")
                .password("password")
                .role(UserRole.USER)
                .name("other")
                .build();
        userRepository.save(otherUser);

        AddImagePathRequest imagePathRequest = new AddImagePathRequest(
                "stores/1/goods/1/images/550e8400-e29b-41d4-a716-446655440000.png");

        Assertions.assertThatThrownBy(() ->
                        storeService.updateImagePath(otherUser.getId(), store.id(), 1L, imagePathRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미지 경로 수정 권한이 없습니다.");
    }

    @Test
    void storeGoods_이미지_경로_수정_상품_없음() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        AddImagePathRequest imagePathRequest = new AddImagePathRequest(
                "stores/1/goods/1/images/550e8400-e29b-41d4-a716-446655440000.png");

        Assertions.assertThatThrownBy(() ->
                        storeService.updateImagePath(testUser.getId(), store.id(), 9999L, imagePathRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 상품이 없습니다.");
    }

    @Test
    void modifyStoreGoods_교차_스토어_수정_실패() {
        // Store A 생성 (admin: testUser) 및 상품 추가
        StoreResponse storeA = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());
        Animation animation = animationRepository.save(animation("슬램덩크"));
        Goods goods = goodsRepository.save(Goods.builder()
                .name("포토카드")
                .animation(animation)
                .build());
        AddExistingStoreGoodsRequest addRequest = new AddExistingStoreGoodsRequest(
                goods.getId(), 5000, 12);
        StoreGoodsResponse storeGoods = storeService.createStoreGoods(addRequest, testUser.getId(), storeA.id());

        // 다른 유저가 소유한 Store B 생성
        User otherAdmin = User.builder()
                .email("other-admin@example.com")
                .password("password")
                .role(UserRole.STORE)
                .name("other-admin")
                .build();
        userRepository.save(otherAdmin);
        StoreResponse storeB = storeService.createStore(
                createStoreRequest(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
                otherAdmin.getId());

        // testUser가 Store B의 goods 수정 시도 → 수정 권한 없음
        Assertions.assertThatThrownBy(() ->
                storeService.modifyStoreGoods(storeB.id(), storeGoods.id(),
                        new UpdateStoreGoodsRequest(6000, 15, null), testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수정 권한이 없습니다.");
    }

    @Test
    void deleteStoreGoods_교차_스토어_삭제_실패() {
        // Store A 생성 (admin: testUser) 및 상품 추가
        StoreResponse storeA = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());
        Animation animation = animationRepository.save(animation("슬램덩크"));
        Goods goods = goodsRepository.save(Goods.builder()
                .name("포토카드")
                .animation(animation)
                .build());
        AddExistingStoreGoodsRequest addRequest = new AddExistingStoreGoodsRequest(
                goods.getId(), 5000, 12);
        StoreGoodsResponse storeGoods = storeService.createStoreGoods(addRequest, testUser.getId(), storeA.id());

        // 다른 유저가 소유한 Store B 생성
        User otherAdmin = User.builder()
                .email("other-admin2@example.com")
                .password("password")
                .role(UserRole.STORE)
                .name("other-admin2")
                .build();
        userRepository.save(otherAdmin);
        StoreResponse storeB = storeService.createStore(
                createStoreRequest(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
                otherAdmin.getId());

        // testUser가 Store B의 goods 삭제 시도 → 삭제 권한 없음
        Assertions.assertThatThrownBy(() ->
                storeService.deleteStoreGoods(storeB.id(), storeGoods.id(), testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("삭제 권한이 없습니다.");
    }

    @Test
    void getStoreDetail_비관리자도_상세_조회_가능() {
        StoreResponse store = storeService.createStore(
                createStoreRequest(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                testUser.getId());

        User regularUser = User.builder()
                .email("regular@example.com")
                .password("password")
                .role(UserRole.USER)
                .name("regular")
                .build();
        userRepository.save(regularUser);

        StoreDetailResponse detail = storeService.getStoreDetail(store.id(), regularUser.getId());

        Assertions.assertThat(detail.id()).isEqualTo(store.id());
        Assertions.assertThat(detail.name()).isEqualTo("애니메이트");
    }

    private CreateStoreRequest createStoreRequest(LocalDate startDate, LocalDate endDate) {
        return new CreateStoreRequest(
                "애니메이트",
                "다 있어요",
                StoreType.POPUP,
                startDate,
                endDate,
                "서울특별시 마포구 양화로 188"
        );
    }

    private Animation animation(String title) {
        Animation animation = newInstance(Animation.class);
        ReflectionTestUtils.setField(animation, "title", title);
        return animation;
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("테스트용 엔티티 생성 실패: " + clazz.getSimpleName(), e);
        }
    }

}
