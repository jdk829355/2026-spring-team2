package team2.goodsmap.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.store.repository.StoreRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final StoreRepository storeRepository;

    // 지역 목록 조회 - GET /api/v1/region
    public List<String> getRegions() {
        return storeRepository.findAllAddresses().stream()
                .map(this::extractRegion)
                .filter(region -> !region.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    // "서울특별시 강남구 테헤란로 123" -> "서울특별시"
    private String extractRegion(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }
        return address.trim().split(" ")[0];
    }
}
