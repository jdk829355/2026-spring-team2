package team2.goodsmap.store.dto.Response;

import lombok.Builder;
import lombok.Getter;
import team2.goodsmap.store.entity.Store;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class StoreResponse {

    private Long id;
    private String name;
    private String description;
    private String type;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private LocalDate startDate;
    private LocalDate endDate;

    public static StoreResponse from(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .description(store.getDescription())
                .type(store.getType().name())
                .address(store.getAddress())
                .lat(store.getLat())
                .lng(store.getLng())
                .startDate(store.getStartDate())
                .endDate(store.getEndDate())
                .build();
    }
}
