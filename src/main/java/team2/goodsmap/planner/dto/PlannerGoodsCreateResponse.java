package team2.goodsmap.planner.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlannerGoodsCreateResponse {
    private Long plannerId;
    private Long plannerGoodsId;
}
