package team2.goodsmap.store.dto.response;

import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.enums.StoreType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StoreResponse(
        Long id,
        String name,
        String description,
        StoreType type,
        LocalDate startDate,
        LocalDate endDate,
        String address,
        BigDecimal lat,
        BigDecimal lng
) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getDescription(),
                store.getType(),
                store.getStartDate(),
                store.getEndDate(),
                store.getAddress(),
                store.getLat(),
                store.getLng()
        );
    }
}
