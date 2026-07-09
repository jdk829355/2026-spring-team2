package team2.goodsmap.store.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team2.goodsmap.global.common.ApiResponse;
import team2.goodsmap.store.service.RegionService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/region")
public class RegionController {

    private final RegionService regionService;

    /**
     * 지역 목록 조회 (지역 탭)
     * GET /api/v1/region
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> getRegions() {
        List<String> result = regionService.getRegions();
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
