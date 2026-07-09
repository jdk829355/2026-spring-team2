package team2.goodsmap.planner.service;

import lombok.RequiredArgsConstructor;
// import team2.goodsmap.global.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.planner.dto.request.PlannerCreateRequest;
import team2.goodsmap.planner.dto.request.PlannerUpdateRequest;
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


    // 내 플래너 조회
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
    //플래너 상세조회
    @Transactional(readOnly = true)
    public PlannerDetailResponse getPlannerDetail(Long userId, Long plannerId) {
        // 1. 플래너 조회 (없으면 404)
        Planner planner = plannerRepository.findById(plannerId)
                .orElseThrow(() -> new NotFoundException("플래너를 찾을 수 없습니다."));

        // 2. 본인 플래너인지 검증 (아니면 403)
        if (!planner.getUser().getId().equals(userId)) {
            //이거 일단 main에 BadRequestException이 없어서 임시로.
            throw new IllegalArgumentException("본인의 플래너만 조회할 수 있습니다.");
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
    //플래너 수정
    @Transactional
    public PlannerResponse updatePlanner(Long userId, Long plannerId, PlannerUpdateRequest request) {
        Planner planner = plannerRepository.findById(plannerId)
                .orElseThrow(() -> new NotFoundException("플래너를 찾을 수 없습니다."));

        // 본인 플래너 검증
        if (!planner.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 플래너만 수정할 수 있습니다.");
        }

        // 전달된 필드만 수정
        planner.update(request.getTitle(), request.getDate());

        return PlannerResponse.from(planner);
    }

    //플래너 삭제
    @Transactional
    public void deletePlanner(Long userId, Long plannerId) {
        Planner planner = plannerRepository.findById(plannerId)
                .orElseThrow(() -> new NotFoundException("플래너를 찾을 수 없습니다."));

        // 본인 플래너 검증
        if (!planner.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 플래너만 삭제할 수 있습니다.");
        }

        plannerRepository.delete(planner);
    }

    //내가 살 것 담기 : 취소(굿즈빼기)
    @Transactional
    public void removeGoods(Long userId, Long plannerId, Long plannerGoodsId) {
        // 1. 플래너 조회 + 본인 것 검증
        Planner planner = plannerRepository.findById(plannerId)
                .orElseThrow(() -> new NotFoundException("플래너를 찾을 수 없습니다."));

        if (!planner.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 플래너만 수정할 수 있습니다.");
        }

        // 2. 그 플래너에 담긴 굿즈가 맞는지 확인
        if (!plannerGoodsRepository.existsByIdAndPlannerId(plannerGoodsId, plannerId)) {
            throw new NotFoundException("해당 플래너에 담긴 굿즈를 찾을 수 없습니다.");
        }

        // 3. 삭제
        plannerGoodsRepository.deleteById(plannerGoodsId);
    }
}