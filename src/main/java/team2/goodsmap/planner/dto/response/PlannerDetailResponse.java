package team2.goodsmap.planner.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class PlannerDetailResponse {
    private Long id;
    private Long userId;
    private String title;
    private LocalDate date;
    private List<PlannerGoodsResponse> goods;
}