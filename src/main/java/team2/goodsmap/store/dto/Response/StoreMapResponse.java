package team2.goodsmap.store.dto.Response;

import lombok.Builder;
import lombok.Getter;
import team2.goodsmap.store.entity.Store;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class StoreMapResponse {

    private Long id;
    private String name;
    private String type;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double distance; // 사용자 위치로부터의 거리 (m)

    public static StoreMapResponse of(Store store, Double distance) {
        return StoreMapResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .type(store.getType().name())
                .address(store.getAddress())
                .lat(store.getLat())
                .lng(store.getLng())
                .startDate(store.getStartDate())
                .endDate(store.getEndDate())
                .distance(distance)
                .build();
    }
}
