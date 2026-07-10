package team2.goodsmap.store.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import team2.goodsmap.global.common.ApiResponse;
import team2.goodsmap.goods.dto.GoodsResponse;
import team2.goodsmap.goods.service.GoodsService;
import team2.goodsmap.store.dto.request.AddExistingStoreGoodsRequest;
import team2.goodsmap.store.dto.request.AddNewStoreGoodsRequest;
import team2.goodsmap.store.dto.request.AddStoreAdminRequest;
import team2.goodsmap.store.dto.request.CreateStoreRequest;
import team2.goodsmap.store.dto.response.StoreAdminResponse;
import team2.goodsmap.store.dto.response.StoreGoodsResponse;
import team2.goodsmap.store.dto.response.StoreResponse;
import team2.goodsmap.store.service.StoreService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class AdminController {
    private final StoreService storeService;
    private final GoodsService goodsService;

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<List<StoreResponse>>> getStores(
            @AuthenticationPrincipal Long userId
    ){
        List<StoreResponse> stores = storeService.getStoreByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateStoreRequest request
            ){
        StoreResponse store = storeService.createStore(request, userId);
        return ResponseEntity.ok(ApiResponse.success(store));
    }

    @PostMapping("/{storeId}/admin")
    public ResponseEntity<ApiResponse<StoreAdminResponse>> createStoreAdmin(
            @PathVariable Long storeId,
            @Valid @RequestBody AddStoreAdminRequest request,
            @AuthenticationPrincipal Long userId
    ){
        StoreAdminResponse storeAdmin = storeService.createStoreAdmin(request, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(storeAdmin));
    }

    @GetMapping("/{storeId}/admin")
    public ResponseEntity<ApiResponse<List<StoreAdminResponse>>> getStoreAdmins(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId
    ){
        List<StoreAdminResponse> storeAdmins = storeService.getStoreAdmin(storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(storeAdmins));
    }

    @DeleteMapping("/{storeId}/admin/{storeAdminId}")
    public ResponseEntity<ApiResponse<Void>> deleteStoreAdmin(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId,
            @PathVariable Long storeAdminId
    ){
        storeService.deleteStoreAdmin(userId, storeId, storeAdminId);
        return ResponseEntity.ok(ApiResponse.success());
    }


    @PostMapping("/{storeId}/goods/new")
    public ResponseEntity<ApiResponse<StoreGoodsResponse>> createStoreGoods(
            @PathVariable Long storeId,
            @Valid @RequestBody AddNewStoreGoodsRequest request,
            @AuthenticationPrincipal Long userId
    ){
        GoodsResponse goodsResponse = goodsService.createGoods(request.goodsInfo());
        StoreGoodsResponse storeGoods = storeService.createStoreGoods(request, goodsResponse, userId, storeId);
        return ResponseEntity.ok(ApiResponse.success(storeGoods));
    }

    @PostMapping("/{storeId}/goods")
    public ResponseEntity<ApiResponse<StoreGoodsResponse>> createStoreGoods(
            @PathVariable Long storeId,
            @Valid @RequestBody AddExistingStoreGoodsRequest request,
            @AuthenticationPrincipal Long userId
    ){
        StoreGoodsResponse storeGoods = storeService.createStoreGoods(request, userId, storeId);
        return ResponseEntity.ok(ApiResponse.success(storeGoods));
    }
}
