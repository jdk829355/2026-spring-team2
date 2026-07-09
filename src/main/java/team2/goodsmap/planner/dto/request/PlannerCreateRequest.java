package team2.goodsmap.planner.dto.request;

import lombok.Getter;
import java.time.LocalDate;

@Getter
public class PlannerCreateRequest {
    private String title;
    private LocalDate date;
}