package team2.goodsmap.goods.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team2.goodsmap.goods.dto.AnimationResponse;
import team2.goodsmap.goods.entity.Animation;
import team2.goodsmap.goods.repository.AnimationRepository;
import team2.goodsmap.support.EntityTestFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnimationServiceTest {

    @Mock
    private AnimationRepository animationRepository;

    @InjectMocks
    private AnimationService animationService;

    @Test
    @DisplayName("keyword가 없으면 전체 작품을 조회한다")
    void getAnimations_전체조회() {
        // given
        List<Animation> animations = List.of(
                EntityTestFactory.animation(1L, "산리오 캐릭터즈"),
                EntityTestFactory.animation(2L, "짱구는 못말려")
        );
        given(animationRepository.findAll()).willReturn(animations);

        // when
        List<AnimationResponse> result = animationService.getAnimations(null);

        // then
        assertThat(result).hasSize(2)
                .extracting(AnimationResponse::getTitle)
                .containsExactly("산리오 캐릭터즈", "짱구는 못말려");
        verify(animationRepository, never()).findByTitleContainingIgnoreCase(any());
    }

    @Test
    @DisplayName("keyword가 있으면 제목으로 검색한다")
    void getAnimations_키워드검색() {
        // given
        given(animationRepository.findByTitleContainingIgnoreCase("산리오"))
                .willReturn(List.of(EntityTestFactory.animation(1L, "산리오 캐릭터즈")));

        // when
        List<AnimationResponse> result = animationService.getAnimations("산리오");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("산리오 캐릭터즈");
    }

    @Test
    @DisplayName("keyword가 빈 문자열이면 전체 조회로 처리한다")
    void getAnimations_빈문자열은_전체조회() {
        // given
        given(animationRepository.findAll()).willReturn(
                List.of(EntityTestFactory.animation(1L, "산리오 캐릭터즈")));

        // when
        List<AnimationResponse> result = animationService.getAnimations("   ");

        // then
        assertThat(result).hasSize(1);
        verify(animationRepository, never()).findByTitleContainingIgnoreCase(any());
    }
}
