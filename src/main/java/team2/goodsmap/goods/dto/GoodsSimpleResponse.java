package team2.goodsmap.goods.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class GoodsSimpleResponse {

    private Long id;
    private String name;
    private Long animationId;
    private String animationTitle;
    private List<String> imageUrls;

}
