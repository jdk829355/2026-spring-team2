package team2.goodsmap.planner.dto;

import jakarta.validation.constraints.NotNull;

public record PlannerGoodsCreateRequest(
        Long plannerId,      // 있으면 이 플래너에 바로 담기 (선택)
        String date,         // plannerId가 없을 때, 이 날짜 기준으로 플래너 찾거나 생성 (yyyy-MM-dd)
        @NotNull(message = "storeGoodsId는 필수입니다.")
        Long storeGoodsId    // 담을 재고 항목 (필수)
) {
}
