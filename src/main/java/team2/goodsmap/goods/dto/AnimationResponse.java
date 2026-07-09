package team2.goodsmap.goods.dto;

import lombok.Builder;
import lombok.Getter;
import team2.goodsmap.goods.entity.Animation;

@Getter
@Builder
public class AnimationResponse {

    private Long id;
    private String title;

    public static AnimationResponse from(Animation animation) {
        return AnimationResponse.builder()
                .id(animation.getId())
                .title(animation.getTitle())
                .build();
    }
}
