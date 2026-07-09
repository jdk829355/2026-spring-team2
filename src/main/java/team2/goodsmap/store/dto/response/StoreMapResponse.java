package team2.goodsmap.store.dto.response;

import team2.goodsmap.store.entity.Store;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StoreMapResponse(
        Long id,
        String name,
        String type,
        String address,
        BigDecimal lat,
        BigDecimal lng,
        LocalDate startDate,
        LocalDate endDate,
        Double distance
) {
    public static StoreMapResponse of(Store store, Double distance) {
        return new StoreMapResponse(
                store.getId(),
                store.getName(),
                store.getType().name(),
                store.getAddress(),
                store.getLat(),
                store.getLng(),
                store.getStartDate(),
                store.getEndDate(),
                distance
        );
    }
}
