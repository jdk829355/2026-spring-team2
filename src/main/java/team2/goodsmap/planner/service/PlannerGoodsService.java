package team2.goodsmap.planner.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.planner.dto.PlannerGoodsCreateRequest;
import team2.goodsmap.planner.dto.PlannerGoodsCreateResponse;
import team2.goodsmap.planner.entity.Planner;
import team2.goodsmap.planner.entity.PlannerGoods;
import team2.goodsmap.planner.repository.PlannerGoodsRepository;
import team2.goodsmap.planner.repository.PlannerRepository;
import team2.goodsmap.store.entity.StoreGoods;
import team2.goodsmap.store.repository.StoreGoodsRepository;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.repository.UserRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class PlannerGoodsService {

    private final PlannerRepository plannerRepository;
    private final PlannerGoodsRepository plannerGoodsRepository;
    private final StoreGoodsRepository storeGoodsRepository;
    private final UserRepository userRepository;

    // 내가 살 것 담기 - POST /api/v1/planner-goods
    public PlannerGoodsCreateResponse addPlannerGoods(Long userId, PlannerGoodsCreateRequest request) {

        // 1. 담을 재고(store_goods)가 실제 존재하는지 확인
        StoreGoods storeGoods = storeGoodsRepository.findById(request.storeGoodsId())
                .orElseThrow(() -> new NotFoundException(
                        "존재하지 않는 재고입니다. id=" + request.storeGoodsId()));

        // 2. plannerId가 왔으면 본인 소유인지 확인, 없으면 date 기준으로 찾거나 새로 만든다
        Planner planner = (request.plannerId() != null)
                ? findOwnedPlanner(userId, request.plannerId())
                : findOrCreatePlanner(userId, request.date());

        // 3. planner_goods 생성
        PlannerGoods plannerGoods = PlannerGoods.builder()
                .planner(planner)
                .storeGoods(storeGoods)
                .build();
        plannerGoodsRepository.save(plannerGoods);

        return PlannerGoodsCreateResponse.builder()
                .plannerId(planner.getId())
                .plannerGoodsId(plannerGoods.getId())
                .build();
    }

    private Planner findOwnedPlanner(Long userId, Long plannerId) {
        Planner planner = plannerRepository.findById(plannerId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 플래너입니다. id=" + plannerId));

        if (!planner.getUser().getId().equals(userId)) {
            throw new NotFoundException("본인의 플래너가 아닙니다. id=" + plannerId);
        }
        return planner;
    }

    private Planner findOrCreatePlanner(Long userId, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);

        return plannerRepository.findByUser_IdAndDate(userId, date)
                .orElseGet(() -> {
                    // 실제 조회 쿼리 없이 FK 연결만 가능한 프록시 참조를 사용
                    // (JWT로 이미 인증된 userId이므로 User 존재는 보장됨)
                    User userRef = userRepository.getReferenceById(userId);

                    Planner newPlanner = Planner.builder()
                            .user(userRef)
                            .date(date)
                            .title(date + " 살 것")
                            .build();

                    return plannerRepository.save(newPlanner);
                });
    }
}
