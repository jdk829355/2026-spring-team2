package team2.goodsmap.planner.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class PlannerSummary {
    private Long id;
    private String title;
    private LocalDate date;
    private long goodsCount;
}