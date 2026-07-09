package team2.goodsmap.store.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team2.goodsmap.global.common.ApiResponse;
import team2.goodsmap.store.dto.Response.StoreGoodsItemResponse;
import team2.goodsmap.store.dto.Response.StoreMapResponse;
import team2.goodsmap.store.dto.Response.StoreResponse;
import team2.goodsmap.store.service.StoreService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;

    /**
     * 스토어 목록 조회 (업체 탭)
     * GET /api/v1/stores?animationId=&region=&keyword=
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreResponse>>> getStores(
            @RequestParam(required = false) Long animationId,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String keyword
    ) {
        List<StoreResponse> result = storeService.getStores(animationId, region, keyword);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 스토어 목록 조회 (지도용)
     * GET /api/v1/stores/map?lat=&lng=&radius=&animationId=&region=
     */
    @GetMapping("/map")
    public ResponseEntity<ApiResponse<List<StoreMapResponse>>> getStoresForMap(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) Long animationId,
            @RequestParam(required = false) String region
    ) {
        List<StoreMapResponse> result = storeService.getStoresForMap(lat, lng, radius, animationId, region);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 매장별 전체 재고 목록 조회
     * GET /api/v1/stores/{storeId}/goods
     */
    @GetMapping("/{storeId}/goods")
    public ResponseEntity<ApiResponse<List<StoreGoodsItemResponse>>> getStoreGoods(@PathVariable Long storeId) {
        List<StoreGoodsItemResponse> result = storeService.getStoreGoods(storeId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
