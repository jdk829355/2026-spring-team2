package team2.goodsmap.planner.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class PlannerListResponse {
    private int totalPlans;
    private int visitDays;
    private List<PlannerSummary> planners;
}