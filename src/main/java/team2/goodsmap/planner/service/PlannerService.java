package team2.goodsmap.planner.service;

import lombok.RequiredArgsConstructor;
import team2.goodsmap.global.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.planner.dto.request.PlannerCreateRequest;
import team2.goodsmap.planner.dto.response.PlannerListResponse;
import team2.goodsmap.planner.dto.response.PlannerResponse;
import team2.goodsmap.planner.entity.Planner;
import team2.goodsmap.planner.repository.PlannerRepository;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.repository.UserRepository;
import team2.goodsmap.planner.dto.response.PlannerSummary;
import team2.goodsmap.planner.repository.PlannerGoodsRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import team2.goodsmap.planner.dto.response.PlannerDetailResponse;
import team2.goodsmap.planner.dto.response.PlannerGoodsResponse;

@Service
@RequiredArgsConstructor
public class PlannerService {


    private final PlannerRepository plannerRepository;
    private final UserRepository userRepository;
    private final PlannerGoodsRepository plannerGoodsRepository;

    // 플래너 생성
    @Transactional
    public PlannerResponse createPlanner(Long userId, PlannerCreateRequest request) {
        // 1. userId로 User 조회 (없으면 예외)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // 2. Planner 생성
        Planner planner = Planner.builder()
                .title(request.getTitle())
                .date(request.getDate())
                .user(user)
                .build();

        // 3. 저장
        Planner saved = plannerRepository.save(planner);

        // 4. 응답 DTO로 변환해서 반환
        return PlannerResponse.from(saved);
    }


    // 메서드 추가
    @Transactional(readOnly = true)
    public PlannerListResponse getMyPlanners(Long userId, String month) {
        // month 없으면 이번 달
        YearMonth ym = (month != null && !month.isBlank())
                ? YearMonth.parse(month)
                : YearMonth.now();

        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Planner> planners = plannerRepository
                .findByUserIdAndDateBetween(userId, start, end);

        // 각 플래너 → 요약 DTO (굿즈 수 포함)
        List<PlannerSummary> summaries = planners.stream()
                .map(p -> PlannerSummary.builder()
                        .id(p.getId())
                        .title(p.getTitle())
                        .date(p.getDate())
                        .goodsCount(plannerGoodsRepository.countByPlannerId(p.getId()))
                        .build())
                .toList();

        // 통계: 전체 플랜 수, 플랜 있는 날짜 수
        int totalPlans = summaries.size();
        int visitDays = (int) planners.stream()
                .map(Planner::getDate)
                .distinct()
                .count();

        return PlannerListResponse.builder()
                .totalPlans(totalPlans)
                .visitDays(visitDays)
                .planners(summaries)
                .build();
    }

    @Transactional(readOnly = true)
    public PlannerDetailResponse getPlannerDetail(Long userId, Long plannerId) {
        // 1. 플래너 조회 (없으면 404)
        Planner planner = plannerRepository.findById(plannerId)
                .orElseThrow(() -> new NotFoundException("플래너를 찾을 수 없습니다."));

        // 2. 본인 플래너인지 검증 (아니면 403)
        if (!planner.getUser().getId().equals(userId)) {
            throw new BadRequestException("본인의 플래너만 조회할 수 있습니다.");
        }

        // 3. 담긴 굿즈 목록 조회 + 굿즈/스토어 정보 조인
        List<PlannerGoodsResponse> goods = plannerGoodsRepository.findByPlannerId(plannerId)
                .stream()
                .map(pg -> {
                    var sg = pg.getStoreGoods();          // store_goods
                    var g = sg.getGoods();                // goods
                    var store = sg.getStore();            // store
                    return PlannerGoodsResponse.builder()
                            .id(pg.getId())
                            .storeGoodsId(sg.getId())
                            .goodsName(g.getName())
                            .animationTitle(g.getAnimation().getTitle())
                            .price(sg.getPrice())
                            .stock(sg.getStock())
                            .imagePath(sg.getImagePath())
                            .store(PlannerGoodsResponse.StoreInfo.builder()
                                    .id(store.getId())
                                    .name(store.getName())
                                    .address(store.getAddress())
                                    .build())
                            .build();
                })
                .toList();

        // 4. 응답 조립
        return PlannerDetailResponse.builder()
                .id(planner.getId())
                .userId(planner.getUser().getId())
                .title(planner.getTitle())
                .date(planner.getDate())
                .goods(goods)
                .build();
    }
}