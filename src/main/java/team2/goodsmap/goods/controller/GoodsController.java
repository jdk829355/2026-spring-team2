package team2.goodsmap.goods.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team2.goodsmap.global.common.ApiResponse;
import team2.goodsmap.goods.dto.GoodsDetailResponse;
import team2.goodsmap.goods.dto.GoodsSimpleResponse;
import team2.goodsmap.goods.service.GoodsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/goods")
public class GoodsController {

    private final GoodsService goodsService;

    /**
     * 상품 목록 조회 (goods 테이블, 등록용)
     * GET /api/v1/goods?q=
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<GoodsSimpleResponse>>> getGoodsForRegistration(
            @RequestParam(required = false) String q
    ) {
        List<GoodsSimpleResponse> result = goodsService.getGoodsForRegistration(q);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 상품 목록 조회 (탐색용)
     * GET /api/v1/goods/search?animationId=&region=&keyword=
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<GoodsSimpleResponse>>> searchGoods(
            @RequestParam(required = false) Long animationId,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String keyword
    ) {
        List<GoodsSimpleResponse> result = goodsService.searchGoods(animationId, region, keyword);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 상품 상세 정보 조회
     * GET /api/v1/goods/{goodsId}
     */
    @GetMapping("/{goodsId}")
    public ResponseEntity<ApiResponse<GoodsDetailResponse>> getGoodsDetail(@PathVariable Long goodsId) {
        GoodsDetailResponse result = goodsService.getGoodsDetail(goodsId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
