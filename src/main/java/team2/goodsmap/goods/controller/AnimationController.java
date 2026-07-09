package team2.goodsmap.goods.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team2.goodsmap.global.common.ApiResponse;
import team2.goodsmap.goods.dto.AnimationResponse;
import team2.goodsmap.goods.service.AnimationService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/animations")
public class AnimationController {

    private final AnimationService animationService;

    /**
     * 작품 목록 조회 및 검색
     * GET /api/v1/animations?keyword=
     */
    @GetMapping
    public ResponseEntity<ApiResponse<GoodsResultResponse>> getAnimations(
            @RequestParam(required = false) String keyword
    ) {
        List<AnimationResponse> result = animationService.getAnimations(keyword);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
