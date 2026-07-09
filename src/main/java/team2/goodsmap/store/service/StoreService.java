package team2.goodsmap.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.global.util.GeoUtils;
import team2.goodsmap.store.dto.StoreGoodsItemResponse;
import team2.goodsmap.store.dto.StoreMapResponse;
import team2.goodsmap.store.dto.StoreResponse;
import team2.goodsmap.store.repository.StoreGoodsRepository;
import team2.goodsmap.store.repository.StoreRepository;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    // radius 파라미터가 없을 때 사용할 기본 반경 (m)
    private static final double DEFAULT_RADIUS_METERS = 3_000.0;

    private final StoreRepository storeRepository;
    private final StoreGoodsRepository storeGoodsRepository;

    // 스토어 목록 조회 - GET /api/v1/stores
    public List<StoreResponse> getStores(Long animationId, String region, String keyword) {
        return storeRepository.searchStores(animationId, region, keyword).stream()
                .map(StoreResponse::from)
                .toList();
    }

    // 스토어 목록 조회(지도용) - GET /api/v1/stores/map
    public List<StoreMapResponse> getStoresForMap(double lat, double lng, Double radius,
                                                  Long animationId, String region) {
        double searchRadius = (radius == null) ? DEFAULT_RADIUS_METERS : radius;

        return storeRepository.findAllForMap(animationId, region).stream()
                .filter(store -> store.getLat() != null && store.getLng() != null)
                .map(store -> {
                    double distance = GeoUtils.distanceMeters(lat, lng, store.getLat(), store.getLng());
                    return StoreMapResponse.of(store, distance);
                })
                .filter(response -> response.getDistance() <= searchRadius)
                .sorted(Comparator.comparingDouble(StoreMapResponse::getDistance))
                .toList();
    }

    // 매장별 전체 재고 목록 조회 - GET /api/v1/stores/{storeId}/goods
    public List<StoreGoodsItemResponse> getStoreGoods(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new NotFoundException("존재하지 않는 스토어입니다. id=" + storeId);
        }

        return storeGoodsRepository.findByStoreId(storeId).stream()
                .map(StoreGoodsItemResponse::from)
                .toList();
    }
}
