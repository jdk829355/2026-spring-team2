package team2.goodsmap.planner.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import team2.goodsmap.global.exception.BadRequestException;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.planner.dto.PlannerGoodsCreateRequest;
import team2.goodsmap.planner.dto.PlannerGoodsCreateResponse;
import team2.goodsmap.planner.entity.Planner;
import team2.goodsmap.planner.entity.PlannerGoods;
import team2.goodsmap.planner.repository.PlannerGoodsRepository;
import team2.goodsmap.planner.repository.PlannerRepository;
import team2.goodsmap.store.entity.StoreGoods;
import team2.goodsmap.store.repository.StoreGoodsRepository;
import team2.goodsmap.support.EntityTestFactory;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.enums.UserRole;
import team2.goodsmap.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PlannerGoodsServiceTest {

    @Mock
    private PlannerRepository plannerRepository;

    @Mock
    private PlannerGoodsRepository plannerGoodsRepository;

    @Mock
    private StoreGoodsRepository storeGoodsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PlannerGoodsService plannerGoodsService;

    private User user(Long id) {
        User user = User.builder().name("고명주").email("gomj@test.com")
                .password("encoded-pw").role(UserRole.USER).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private StoreGoods storeGoods(Long id) {
        return EntityTestFactory.storeGoods(id, 15000, 30, "img.png", null, null);
    }

    @Test
    @DisplayName("plannerId와 date가 둘 다 없으면 BadRequestException이 발생한다")
    void plannerId와_date_둘다없으면_예외() {
        // given
        PlannerGoodsCreateRequest request = new PlannerGoodsCreateRequest(null, null, 58L);

        // when & then
        assertThatThrownBy(() -> plannerGoodsService.addPlannerGoods(1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("존재하지 않는 재고를 담으려 하면 NotFoundException이 발생한다")
    void 존재하지않는_재고면_예외() {
        // given
        given(storeGoodsRepository.findById(999L)).willReturn(Optional.empty());
        PlannerGoodsCreateRequest request = new PlannerGoodsCreateRequest(null, "2026-07-15", 999L);

        // when & then
        assertThatThrownBy(() -> plannerGoodsService.addPlannerGoods(1L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("plannerId가 주어지고 본인 소유면 그대로 담는다")
    void 본인_플래너에_담기_성공() {
        // given
        Long userId = 1L;
        User owner = user(userId);

        Planner planner = Planner.builder()
                .title("7/15 살 것").date(LocalDate.of(2026, 7, 15)).user(owner).build();
        ReflectionTestUtils.setField(planner, "id", 21L);

        StoreGoods storeGoods = storeGoods(58L);

        given(storeGoodsRepository.findById(58L)).willReturn(Optional.of(storeGoods));
        given(plannerRepository.findById(21L)).willReturn(Optional.of(planner));
        given(plannerGoodsRepository.save(any(PlannerGoods.class))).willAnswer(invocation -> {
            PlannerGoods pg = invocation.getArgument(0);
            ReflectionTestUtils.setField(pg, "id", 103L);
            return pg;
        });

        PlannerGoodsCreateRequest request = new PlannerGoodsCreateRequest(21L, null, 58L);

        // when
        PlannerGoodsCreateResponse response = plannerGoodsService.addPlannerGoods(userId, request);

        // then
        assertThat(response.getPlannerId()).isEqualTo(21L);
        assertThat(response.getPlannerGoodsId()).isEqualTo(103L);
    }

    @Test
    @DisplayName("본인 소유가 아닌 플래너에 담으려 하면 NotFoundException이 발생한다")
    void 본인소유아닌_플래너면_예외() {
        // given
        User owner = user(2L); // 다른 사람 소유

        Planner planner = Planner.builder()
                .title("t").date(LocalDate.now()).user(owner).build();
        ReflectionTestUtils.setField(planner, "id", 21L);

        given(storeGoodsRepository.findById(58L)).willReturn(Optional.of(storeGoods(58L)));
        given(plannerRepository.findById(21L)).willReturn(Optional.of(planner));

        PlannerGoodsCreateRequest request = new PlannerGoodsCreateRequest(21L, null, 58L);

        // when & then
        assertThatThrownBy(() -> plannerGoodsService.addPlannerGoods(1L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("본인");
    }

    @Test
    @DisplayName("존재하지 않는 plannerId를 지정하면 NotFoundException이 발생한다")
    void 존재하지않는_플래너id면_예외() {
        // given
        given(storeGoodsRepository.findById(58L)).willReturn(Optional.of(storeGoods(58L)));
        given(plannerRepository.findById(999L)).willReturn(Optional.empty());

        PlannerGoodsCreateRequest request = new PlannerGoodsCreateRequest(999L, null, 58L);

        // when & then
        assertThatThrownBy(() -> plannerGoodsService.addPlannerGoods(1L, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("plannerId 없이 date만 주어지고 해당 날짜 플래너가 없으면 새로 생성한다")
    void 날짜기준_플래너_신규생성() {
        // given
        Long userId = 1L;
        LocalDate date = LocalDate.of(2026, 7, 20);

        given(storeGoodsRepository.findById(58L)).willReturn(Optional.of(storeGoods(58L)));
        given(plannerRepository.findByUser_IdAndDate(userId, date)).willReturn(Optional.empty());

        User userRef = user(userId);
        given(userRepository.getReferenceById(userId)).willReturn(userRef);

        given(plannerRepository.save(any(Planner.class))).willAnswer(invocation -> {
            Planner p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 99L);
            return p;
        });
        given(plannerGoodsRepository.save(any(PlannerGoods.class))).willAnswer(invocation -> {
            PlannerGoods pg = invocation.getArgument(0);
            ReflectionTestUtils.setField(pg, "id", 200L);
            return pg;
        });

        PlannerGoodsCreateRequest request = new PlannerGoodsCreateRequest(null, "2026-07-20", 58L);

        // when
        PlannerGoodsCreateResponse response = plannerGoodsService.addPlannerGoods(userId, request);

        // then
        assertThat(response.getPlannerId()).isEqualTo(99L);
        assertThat(response.getPlannerGoodsId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("해당 날짜의 플래너가 이미 있으면 새로 만들지 않고 재사용한다")
    void 날짜기준_기존플래너_재사용() {
        // given
        Long userId = 1L;
        LocalDate date = LocalDate.of(2026, 7, 20);
        User owner = user(userId);

        Planner existing = Planner.builder().title("기존").date(date).user(owner).build();
        ReflectionTestUtils.setField(existing, "id", 55L);

        given(storeGoodsRepository.findById(58L)).willReturn(Optional.of(storeGoods(58L)));
        given(plannerRepository.findByUser_IdAndDate(userId, date)).willReturn(Optional.of(existing));
        given(plannerGoodsRepository.save(any(PlannerGoods.class))).willAnswer(invocation -> {
            PlannerGoods pg = invocation.getArgument(0);
            ReflectionTestUtils.setField(pg, "id", 300L);
            return pg;
        });

        PlannerGoodsCreateRequest request = new PlannerGoodsCreateRequest(null, "2026-07-20", 58L);

        // when
        PlannerGoodsCreateResponse response = plannerGoodsService.addPlannerGoods(userId, request);

        // then: 기존 플래너(55L)를 그대로 사용, 새로 저장(save)하지 않음
        assertThat(response.getPlannerId()).isEqualTo(55L);
        org.mockito.Mockito.verify(plannerRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("date 형식이 잘못되면 BadRequestException이 발생한다")
    void 날짜형식_오류시_예외() {
        // given
        given(storeGoodsRepository.findById(58L)).willReturn(Optional.of(storeGoods(58L)));
        PlannerGoodsCreateRequest request = new PlannerGoodsCreateRequest(null, "2026-13-99", 58L);

        // when & then
        assertThatThrownBy(() -> plannerGoodsService.addPlannerGoods(1L, request))
                .isInstanceOf(BadRequestException.class);
    }
}
