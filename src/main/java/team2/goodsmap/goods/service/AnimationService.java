package team2.goodsmap.goods.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.goods.dto.AnimationResponse;
import team2.goodsmap.goods.entity.Animation;
import team2.goodsmap.goods.repository.AnimationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnimationService {

    private final AnimationRepository animationRepository;

    // 작품 목록 조회 및 검색 - GET /api/v1/animations
    public List<AnimationResponse> getAnimations(String keyword) {
        List<Animation> animations = (keyword == null || keyword.isBlank())
                ? animationRepository.findAll()
                : animationRepository.findByTitleContainingIgnoreCase(keyword);

        return animations.stream()
                .map(AnimationResponse::from)
                .toList();
    }
}
