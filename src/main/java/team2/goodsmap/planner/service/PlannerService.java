package team2.goodsmap.planner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.global.exception.BadRequestException;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.planner.dto.request.PlannerCreateRequest;
import team2.goodsmap.planner.dto.request.PlannerUpdateRequest;
import team2.goodsmap.planner.dto.response.PlannerDetailResponse;
import team2.goodsmap.planner.dto.response.PlannerGoodsResponse;
import team2.goodsmap.planner.dto.response.PlannerListResponse;
import team2.goodsmap.planner.dto.response.PlannerResponse;
import team2.goodsmap.planner.dto.response.PlannerSummary;
import team2.goodsmap.planner.entity.Planner;
import team2.goodsmap.planner.repository.PlannerGoodsRepository;
import team2.goodsmap.planner.repository.PlannerRepository;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
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
        log.info("[플래너생성] userId={}, plannerId={}, date={}",
                userId, saved.getId(), saved.getDate());

        // 4. 응답 DTO로 변환해서 반환
        return PlannerResponse.from(saved);
    }

    // 내 플래너 목록 조회
    @Transactional(readOnly = true)
    public PlannerListResponse getMyPlanners(Long userId, String month) {
        // month 없으면 이번 달, 있으면 파싱 (형식 틀리면 400)
        YearMonth ym = parseMonthOrCurrent(month);

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

    // 플래너 상세 조회
    @Transactional(readOnly = true)
    public PlannerDetailResponse getPlannerDetail(Long userId, Long plannerId) {
        // 1. 플래너 조회 + 본인 것인지 검증
        Planner planner = findOwnedPlanner(userId, plannerId);

        // 2. 담긴 굿즈 목록 조회 (store_goods → goods → animation, store 까지 fetch join)
        List<PlannerGoodsResponse> goods = plannerGoodsRepository.findByPlannerIdWithDetails(plannerId)
                .stream()
                .map(PlannerGoodsResponse::from)
                .toList();

        // 3. 응답 조립
        return PlannerDetailResponse.builder()
                .id(planner.getId())
                .userId(planner.getUser().getId())
                .title(planner.getTitle())
                .date(planner.getDate())
                .goods(goods)
                .build();
    }

    // 플래너 수정
    @Transactional
    public PlannerResponse updatePlanner(Long userId, Long plannerId, PlannerUpdateRequest request) {
        Planner planner = findOwnedPlanner(userId, plannerId);

        // 전달된 필드만 수정
        planner.update(request.getTitle(), request.getDate());
        log.info("[플래너수정] userId={}, plannerId={}", userId, plannerId);

        return PlannerResponse.from(planner);
    }

    // 플래너 삭제
    @Transactional
    public void deletePlanner(Long userId, Long plannerId) {
        Planner planner = findOwnedPlanner(userId, plannerId);

        plannerRepository.delete(planner);
        log.info("[플래너삭제] userId={}, plannerId={}", userId, plannerId);
    }

    // 내가 살 것 담기 : 취소 (굿즈 빼기)
    @Transactional
    public void removeGoods(Long userId, Long plannerId, Long plannerGoodsId) {
        // 1. 플래너 조회 + 본인 것 검증
        findOwnedPlanner(userId, plannerId);

        // 2. 그 플래너에 담긴 굿즈가 맞는지 확인
        if (!plannerGoodsRepository.existsByIdAndPlannerId(plannerGoodsId, plannerId)) {
            throw new NotFoundException("해당 플래너에 담긴 굿즈를 찾을 수 없습니다.");
        }

        // 3. 삭제
        plannerGoodsRepository.deleteById(plannerGoodsId);
        log.info("[굿즈취소] userId={}, plannerId={}, plannerGoodsId={}",
                userId, plannerId, plannerGoodsId);
    }

    // ===== private helpers =====

    /*
     * 플래너를 찾고, 로그인한 유저의 것이 맞는지까지 확인한다.
     * 5개 API에서 똑같이 반복되던 조회 + 권한검증을 한 곳으로 모음.

     * 남의 플래너일 때도 403이 아니라 404를 던진다.
     * "그 id의 플래너가 존재하긴 한다"는 사실 자체를 숨기기 위함(= 존재 여부 노출 방지).
     * PlannerGoodsService.findOwnedPlanner()와 동일한 정책.
     */
    private Planner findOwnedPlanner(Long userId, Long plannerId) {
        Planner planner = plannerRepository.findById(plannerId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 플래너입니다. id=" + plannerId));

        if (!planner.getUser().getId().equals(userId)) {
            throw new NotFoundException("본인의 플래너가 아닙니다. id=" + plannerId);
        }
        return planner;
    }

    /**
     * "2026-07" 같은 month 파라미터를 파싱한다.
     * 값이 없으면 이번 달, 형식이 틀리면 500이 아니라 400이 나가도록 BadRequestException으로 변환.
     */
    private YearMonth parseMonthOrCurrent(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("month 형식이 올바르지 않습니다. (yyyy-MM) 입력값: " + month);
        }
    }
}
