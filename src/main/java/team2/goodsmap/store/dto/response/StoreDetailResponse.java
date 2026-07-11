package team2.goodsmap.store.dto.response;

import team2.goodsmap.store.enums.StoreType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StoreDetailResponse(
        Long id,
        String name,
        String description,
        StoreType type,
        LocalDate startDate,
        LocalDate endDate,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        Long goodsCount
) {
}
