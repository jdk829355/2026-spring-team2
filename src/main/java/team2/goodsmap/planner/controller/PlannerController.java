package team2.goodsmap.planner.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import team2.goodsmap.global.common.ApiResponse;
import team2.goodsmap.planner.dto.request.PlannerCreateRequest;
import team2.goodsmap.planner.dto.response.PlannerDetailResponse;
import team2.goodsmap.planner.dto.response.PlannerListResponse;
import team2.goodsmap.planner.dto.response.PlannerResponse;
import team2.goodsmap.planner.service.PlannerService;

@RestController
@RequestMapping("/api/v1/planners")
@RequiredArgsConstructor
public class PlannerController {

    private final PlannerService plannerService;

    // 플래너 생성
    @PostMapping
    public ResponseEntity<ApiResponse<PlannerResponse>> createPlanner(
            @AuthenticationPrincipal Long userId,
            @RequestBody PlannerCreateRequest request) {
        PlannerResponse response = plannerService.createPlanner(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 내 플래너 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<PlannerListResponse>> getMyPlanners(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String month) {
        PlannerListResponse response = plannerService.getMyPlanners(userId, month);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    // 플래너 상세 조회
    @GetMapping("/{plannerId}")
    public ResponseEntity<ApiResponse<PlannerDetailResponse>> getPlannerDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long plannerId) {
        PlannerDetailResponse response = plannerService.getPlannerDetail(userId, plannerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}