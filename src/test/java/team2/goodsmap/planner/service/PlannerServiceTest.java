package team2.goodsmap.planner.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import team2.goodsmap.global.exception.BadRequestException;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.planner.dto.request.PlannerCreateRequest;
import team2.goodsmap.planner.dto.request.PlannerUpdateRequest;
import team2.goodsmap.planner.dto.response.PlannerDetailResponse;
import team2.goodsmap.planner.dto.response.PlannerListResponse;
import team2.goodsmap.planner.dto.response.PlannerResponse;
import team2.goodsmap.planner.entity.Planner;
import team2.goodsmap.planner.entity.PlannerGoods;
import team2.goodsmap.planner.repository.PlannerGoodsRepository;
import team2.goodsmap.planner.repository.PlannerRepository;
import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.entity.StoreGoods;
import team2.goodsmap.store.enums.StoreType;
import team2.goodsmap.support.EntityTestFactory;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.enums.UserRole;
import team2.goodsmap.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlannerServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long PLANNER_ID = 21L;

    @Mock
    private PlannerRepository plannerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlannerGoodsRepository plannerGoodsRepository;

    @InjectMocks
    private PlannerService plannerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(plannerService, "cdnUrl", "cdn.example.com");
    }

    // ===== fixtures =====

    private User user(Long id) {
        User user = User.builder().name("김민서").email("toto@test.com")
                .password("encoded-pw").role(UserRole.USER).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Planner planner(Long id, Long ownerId, LocalDate date) {
        Planner planner = Planner.builder()
                .title("7월 굿즈 투어")
                .date(date)
                .user(user(ownerId))
                .build();
        ReflectionTestUtils.setField(planner, "id", id);
        return planner;
    }

    private PlannerGoods plannerGoods(Long id, Planner planner) {
        Store store = EntityTestFactory.store(7L, "애니메이트 홍대점", StoreType.STORE,
                "서울 마포구 양화로 100", new BigDecimal("37.5563"), new BigDecimal("126.9236"));
        StoreGoods storeGoods = EntityTestFactory.storeGoods(
                58L,
                15000,
                30,
                "stores/7/goods/58/images/57165dce-ae65-4da2-9d5b-69747ce06381.png",
                EntityTestFactory.goods(11L, "아크릴 스탠드",
                        EntityTestFactory.animation(3L, "주술회전")),
                store
        );

        PlannerGoods pg = PlannerGoods.builder().planner(planner).storeGoods(storeGoods).build();
        ReflectionTestUtils.setField(pg, "id", id);
        return pg;
    }

    // ===== 플래너 생성 =====

    @Test
    @DisplayName("플래너를 생성하면 저장된 플래너 정보를 반환한다")
    void 플래너_생성_성공() {
        // given
        PlannerCreateRequest request = new PlannerCreateRequest();
        ReflectionTestUtils.setField(request, "title", "7월 굿즈 투어");
        ReflectionTestUtils.setField(request, "date", LocalDate.of(2026, 7, 15));

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
        given(plannerRepository.save(any(Planner.class))).willAnswer(invocation -> {
            Planner p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", PLANNER_ID);
            return p;
        });

        // when
        PlannerResponse response = plannerService.createPlanner(USER_ID, request);

        // then
        assertThat(response.getId()).isEqualTo(PLANNER_ID);
        assertThat(response.getTitle()).isEqualTo("7월 굿즈 투어");
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 7, 15));
    }

    @Test
    @DisplayName("존재하지 않는 유저가 플래너를 생성하려 하면 NotFoundException이 발생한다")
    void 플래너_생성_유저없으면_예외() {
        // given
        PlannerCreateRequest request = new PlannerCreateRequest();
        ReflectionTestUtils.setField(request, "title", "제목");
        ReflectionTestUtils.setField(request, "date", LocalDate.now());

        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> plannerService.createPlanner(999L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("사용자");
    }

    // ===== 목록 조회 =====

    @Test
    @DisplayName("month를 지정하면 해당 월의 플래너와 통계를 반환한다")
    void 목록조회_월지정_성공() {
        // given: 7/15에 2개, 7/20에 1개 → 전체 3개, 방문일수 2일
        LocalDate d1 = LocalDate.of(2026, 7, 15);
        LocalDate d2 = LocalDate.of(2026, 7, 20);
        List<Planner> planners = List.of(
                planner(1L, USER_ID, d1),
                planner(2L, USER_ID, d1),
                planner(3L, USER_ID, d2)
        );

        given(plannerRepository.findByUserIdAndDateBetween(
                USER_ID, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .willReturn(planners);
        given(plannerGoodsRepository.countByPlannerId(any())).willReturn(2L);

        // when
        PlannerListResponse response = plannerService.getMyPlanners(USER_ID, "2026-07");

        // then
        assertThat(response.getTotalPlans()).isEqualTo(3);
        assertThat(response.getVisitDays()).isEqualTo(2);   // 중복 날짜는 하루로 셈
        assertThat(response.getPlanners()).hasSize(3);
        assertThat(response.getPlanners().get(0).getGoodsCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("month가 없으면 이번 달을 조회한다")
    void 목록조회_month없으면_이번달() {
        // given
        YearMonth now = YearMonth.now();
        given(plannerRepository.findByUserIdAndDateBetween(
                USER_ID, now.atDay(1), now.atEndOfMonth()))
                .willReturn(List.of());

        // when
        PlannerListResponse response = plannerService.getMyPlanners(USER_ID, null);

        // then
        assertThat(response.getTotalPlans()).isZero();
        assertThat(response.getVisitDays()).isZero();
        assertThat(response.getPlanners()).isEmpty();
    }

    @Test
    @DisplayName("month 형식이 잘못되면 500이 아니라 BadRequestException(400)이 발생한다")
    void 목록조회_month형식오류시_400() {
        // when & then: 13월은 존재하지 않음
        assertThatThrownBy(() -> plannerService.getMyPlanners(USER_ID, "2026-13"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("month");

        // "2026/07" 처럼 구분자가 다른 경우도 400
        assertThatThrownBy(() -> plannerService.getMyPlanners(USER_ID, "2026/07"))
                .isInstanceOf(BadRequestException.class);

        // DB까지 가지 않고 파싱 단계에서 막힌다
        verify(plannerRepository, never()).findByUserIdAndDateBetween(any(), any(), any());
    }

    // ===== 상세 조회 =====

    @Test
    @DisplayName("상세 조회하면 담긴 굿즈와 스토어 정보까지 함께 반환한다")
    void 상세조회_성공() {
        // given
        Planner planner = planner(PLANNER_ID, USER_ID, LocalDate.of(2026, 7, 15));

        given(plannerRepository.findById(PLANNER_ID)).willReturn(Optional.of(planner));
        given(plannerGoodsRepository.findByPlannerIdWithDetails(PLANNER_ID))
                .willReturn(List.of(plannerGoods(103L, planner)));

        // when
        PlannerDetailResponse response = plannerService.getPlannerDetail(USER_ID, PLANNER_ID);

        // then
        assertThat(response.getId()).isEqualTo(PLANNER_ID);
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getGoods()).hasSize(1);

        var goods = response.getGoods().get(0);
        assertThat(goods.getId()).isEqualTo(103L);
        assertThat(goods.getStoreGoodsId()).isEqualTo(58L);
        assertThat(goods.getGoodsName()).isEqualTo("아크릴 스탠드");
        assertThat(goods.getAnimationTitle()).isEqualTo("주술회전");
        assertThat(goods.getPrice()).isEqualTo(15000);
        assertThat(goods.getStock()).isEqualTo(30);
        assertThat(goods.getImagePath()).isEqualTo(
                "https://cdn.example.com/stores/7/goods/58/images/57165dce-ae65-4da2-9d5b-69747ce06381.png"
        );
        assertThat(goods.getStore().getName()).isEqualTo("애니메이트 홍대점");
        assertThat(goods.getStore().getAddress()).isEqualTo("서울 마포구 양화로 100");
    }

    @Test
    @DisplayName("담긴 굿즈가 없어도 빈 목록으로 정상 조회된다")
    void 상세조회_굿즈없어도_성공() {
        // given
        Planner planner = planner(PLANNER_ID, USER_ID, LocalDate.of(2026, 7, 15));

        given(plannerRepository.findById(PLANNER_ID)).willReturn(Optional.of(planner));
        given(plannerGoodsRepository.findByPlannerIdWithDetails(PLANNER_ID)).willReturn(List.of());

        // when
        PlannerDetailResponse response = plannerService.getPlannerDetail(USER_ID, PLANNER_ID);

        // then
        assertThat(response.getGoods()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 플래너를 조회하면 NotFoundException이 발생한다")
    void 상세조회_없는플래너면_404() {
        // given
        given(plannerRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> plannerService.getPlannerDetail(USER_ID, 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("남의 플래너를 조회하면 NotFoundException이 발생한다 (존재 여부를 숨김)")
    void 상세조회_남의플래너면_404() {
        // given: 플래너 주인은 2번 유저인데 1번 유저가 조회 시도
        given(plannerRepository.findById(PLANNER_ID))
                .willReturn(Optional.of(planner(PLANNER_ID, OTHER_USER_ID, LocalDate.now())));

        // when & then
        assertThatThrownBy(() -> plannerService.getPlannerDetail(USER_ID, PLANNER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("본인");
    }

    // ===== 수정 =====

    @Test
    @DisplayName("title만 보내면 date는 그대로 두고 title만 수정된다")
    void 수정_부분수정_title만() {
        // given
        Planner planner = planner(PLANNER_ID, USER_ID, LocalDate.of(2026, 7, 15));
        given(plannerRepository.findById(PLANNER_ID)).willReturn(Optional.of(planner));

        PlannerUpdateRequest request = new PlannerUpdateRequest();
        ReflectionTestUtils.setField(request, "title", "수정된 제목");

        // when
        PlannerResponse response = plannerService.updatePlanner(USER_ID, PLANNER_ID, request);

        // then
        assertThat(response.getTitle()).isEqualTo("수정된 제목");
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 7, 15)); // 안 바뀜
    }

    @Test
    @DisplayName("date만 보내면 title은 그대로 두고 date만 수정된다")
    void 수정_부분수정_date만() {
        // given
        Planner planner = planner(PLANNER_ID, USER_ID, LocalDate.of(2026, 7, 15));
        given(plannerRepository.findById(PLANNER_ID)).willReturn(Optional.of(planner));

        PlannerUpdateRequest request = new PlannerUpdateRequest();
        ReflectionTestUtils.setField(request, "date", LocalDate.of(2026, 8, 1));

        // when
        PlannerResponse response = plannerService.updatePlanner(USER_ID, PLANNER_ID, request);

        // then
        assertThat(response.getTitle()).isEqualTo("7월 굿즈 투어"); // 안 바뀜
        assertThat(response.getDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    @DisplayName("남의 플래너를 수정하면 NotFoundException이 발생한다")
    void 수정_남의플래너면_404() {
        // given
        given(plannerRepository.findById(PLANNER_ID))
                .willReturn(Optional.of(planner(PLANNER_ID, OTHER_USER_ID, LocalDate.now())));

        PlannerUpdateRequest request = new PlannerUpdateRequest();
        ReflectionTestUtils.setField(request, "title", "탈취 시도");

        // when & then
        assertThatThrownBy(() -> plannerService.updatePlanner(USER_ID, PLANNER_ID, request))
                .isInstanceOf(NotFoundException.class);
    }

    // ===== 삭제 =====

    @Test
    @DisplayName("본인 플래너를 삭제하면 repository.delete가 호출된다")
    void 삭제_성공() {
        // given
        Planner planner = planner(PLANNER_ID, USER_ID, LocalDate.now());
        given(plannerRepository.findById(PLANNER_ID)).willReturn(Optional.of(planner));

        // when
        plannerService.deletePlanner(USER_ID, PLANNER_ID);

        // then
        verify(plannerRepository).delete(planner);
    }

    @Test
    @DisplayName("남의 플래너를 삭제하려 하면 NotFoundException이 발생하고 삭제되지 않는다")
    void 삭제_남의플래너면_404() {
        // given
        given(plannerRepository.findById(PLANNER_ID))
                .willReturn(Optional.of(planner(PLANNER_ID, OTHER_USER_ID, LocalDate.now())));

        // when & then
        assertThatThrownBy(() -> plannerService.deletePlanner(USER_ID, PLANNER_ID))
                .isInstanceOf(NotFoundException.class);

        verify(plannerRepository, never()).delete(any());
    }

    // ===== 굿즈 취소 =====

    @Test
    @DisplayName("담긴 굿즈를 취소하면 planner_goods가 삭제된다")
    void 굿즈취소_성공() {
        // given
        given(plannerRepository.findById(PLANNER_ID))
                .willReturn(Optional.of(planner(PLANNER_ID, USER_ID, LocalDate.now())));
        given(plannerGoodsRepository.existsByIdAndPlannerId(103L, PLANNER_ID)).willReturn(true);

        // when
        plannerService.removeGoods(USER_ID, PLANNER_ID, 103L);

        // then
        verify(plannerGoodsRepository).deleteById(103L);
    }

    @Test
    @DisplayName("다른 플래너에 담긴 굿즈를 취소하려 하면 NotFoundException이 발생한다")
    void 굿즈취소_해당플래너에_없으면_404() {
        // given
        given(plannerRepository.findById(PLANNER_ID))
                .willReturn(Optional.of(planner(PLANNER_ID, USER_ID, LocalDate.now())));
        given(plannerGoodsRepository.existsByIdAndPlannerId(999L, PLANNER_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> plannerService.removeGoods(USER_ID, PLANNER_ID, 999L))
                .isInstanceOf(NotFoundException.class);

        verify(plannerGoodsRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("남의 플래너에서 굿즈를 빼려 하면 NotFoundException이 발생한다")
    void 굿즈취소_남의플래너면_404() {
        // given
        given(plannerRepository.findById(PLANNER_ID))
                .willReturn(Optional.of(planner(PLANNER_ID, OTHER_USER_ID, LocalDate.now())));

        // when & then
        assertThatThrownBy(() -> plannerService.removeGoods(USER_ID, PLANNER_ID, 103L))
                .isInstanceOf(NotFoundException.class);

        verify(plannerGoodsRepository, never()).deleteById(any());
    }
}
