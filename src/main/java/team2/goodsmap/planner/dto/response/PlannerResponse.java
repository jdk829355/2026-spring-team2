package team2.goodsmap.planner.dto.response;

import lombok.Builder;
import lombok.Getter;
import team2.goodsmap.planner.entity.Planner;
import java.time.LocalDate;

@Getter
@Builder
public class PlannerResponse {
    private Long id;
    private Long userId;
    private String title;
    private LocalDate date;

    public static PlannerResponse from(Planner planner) {
        return PlannerResponse.builder()
                .id(planner.getId())
                .userId(planner.getUser().getId())
                .title(planner.getTitle())
                .date(planner.getDate())
                .build();
    }
}