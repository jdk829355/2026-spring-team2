package team2.goodsmap.planner.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team2.goodsmap.global.common.ApiResponse;
import team2.goodsmap.planner.dto.PlannerGoodsCreateRequest;
import team2.goodsmap.planner.dto.PlannerGoodsCreateResponse;
import team2.goodsmap.planner.service.PlannerGoodsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/planner-goods")
public class PlannerGoodsController {

    private final PlannerGoodsService plannerGoodsService;

    /**
     * 내가 살 것 담기
     * POST /api/v1/planner-goods
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createPlannerGoods(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PlannerGoodsCreateRequest requestDto
    ) {
        PlannerGoodsCreateResponse response = plannerGoodsService.addPlannerGoods(userId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
