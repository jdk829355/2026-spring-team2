package team2.goodsmap.store.dto.request;

import team2.goodsmap.store.enums.StoreType;

import java.math.BigDecimal;
import java.time.LocalDate;



public record CreateStoreRequest(
        String name,
        String description,
        StoreType type,
        LocalDate startDate,
        LocalDate endDate,
        String address,
        BigDecimal lat,
        BigDecimal lng
) {
    // TODO: 이거 위경도는 프론트에서 지오코딩해서 넘겨주거나 백에서 별도 서비스로 지오코딩 해야할 것 같습니다.
    // 일단 요청으로 위경도 받도록 남겨두겠습니다.
}
