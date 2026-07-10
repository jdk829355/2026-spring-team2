package team2.goodsmap.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import team2.goodsmap.store.enums.StoreType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateStoreRequest(
        @NotBlank(message = "업체명은 필수입니다.")
        String name,
        String description,
        @NotNull(message = "업체 타입은 필수입니다.")
        StoreType type,
        LocalDate startDate,
        LocalDate endDate,
        @NotBlank(message = "주소는 필수입니다.")
        String address,
        @NotNull(message = "위도는 필수입니다.")
        BigDecimal lat,
        @NotNull(message = "경도는 필수입니다.")
        BigDecimal lng
) {
    // TODO: 이거 위경도는 프론트에서 지오코딩해서 넘겨주거나 백에서 별도 서비스로 지오코딩 해야할 것 같습니다.
    // 일단 요청으로 위경도 받도록 남겨두겠습니다.
}
