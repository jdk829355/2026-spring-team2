package team2.goodsmap.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import team2.goodsmap.store.enums.StoreType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateStoreRequest(
        String name,
        String description,
        StoreType type,
        LocalDate startDate,
        LocalDate endDate,
        String address
) {
}
