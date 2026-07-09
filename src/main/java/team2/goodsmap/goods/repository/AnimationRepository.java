package team2.goodsmap.goods.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team2.goodsmap.goods.entity.Animation;

import java.util.List;

public interface AnimationRepository extends JpaRepository<Animation, Long> {

    List<Animation> findByTitleContainingIgnoreCase(String keyword);
}
