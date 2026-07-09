package team2.goodsmap.planner.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.planner.dto.request.PlannerCreateRequest;
import team2.goodsmap.planner.dto.response.PlannerResponse;
import team2.goodsmap.planner.entity.Planner;
import team2.goodsmap.planner.repository.PlannerRepository;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class PlannerService {

    private final PlannerRepository plannerRepository;
    private final UserRepository userRepository;

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
}