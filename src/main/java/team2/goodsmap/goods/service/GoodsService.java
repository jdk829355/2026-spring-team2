package team2.goodsmap.goods.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.goods.dto.CreateGoodsRequest;
import team2.goodsmap.goods.dto.GoodsDetailResponse;
import team2.goodsmap.goods.dto.GoodsResponse;
import team2.goodsmap.goods.dto.GoodsSimpleResponse;
import team2.goodsmap.goods.entity.Animation;
import team2.goodsmap.goods.entity.Goods;
import team2.goodsmap.goods.repository.AnimationRepository;
import team2.goodsmap.goods.repository.GoodsRepository;
import team2.goodsmap.store.entity.StoreGoods;
import team2.goodsmap.store.repository.StoreGoodsRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoodsService {

    private final GoodsRepository goodsRepository;
    private final StoreGoodsRepository storeGoodsRepository;
    private final AnimationRepository animationRepository;

    // 상품 목록 조회 (goods 테이블, 등록용) - GET /api/v1/goods?q
    public List<GoodsSimpleResponse> getGoodsForRegistration(String q) {
        List<Goods> goodsList = (q == null || q.isBlank())
                ? goodsRepository.findAll()
                : goodsRepository.findByNameContainingIgnoreCase(q);

        return goodsList.stream()
                .map(GoodsSimpleResponse::from)
                .toList();
    }

    // 상품 목록 조회 (탐색용) - GET /api/v1/goods/search
    public List<GoodsSimpleResponse> searchGoods(Long animationId, String region, String keyword) {
        return goodsRepository.searchGoods(animationId, region, keyword).stream()
                .map(GoodsSimpleResponse::from)
                .toList();
    }

    // 상품 상세 정보 조회 - GET /api/v1/goods/{goodsId}
    public GoodsDetailResponse getGoodsDetail(Long goodsId) {
        Goods goods = goodsRepository.findById(goodsId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 상품입니다. id=" + goodsId));

        List<StoreGoods> storeGoodsList = storeGoodsRepository.findByGoodsId(goodsId);

        List<GoodsDetailResponse.StoreSummary> stores = storeGoodsList.stream()
                .map(sg -> GoodsDetailResponse.StoreSummary.builder()
                        .storeGoodsId(sg.getId())
                        .storeId(sg.getStore().getId())
                        .storeName(sg.getStore().getName())
                        .address(sg.getStore().getAddress())
                        .price(sg.getPrice())
                        .stock(sg.getStock())
                        .imagePath(sg.getImagePath())
                        .build())
                .toList();

        return GoodsDetailResponse.builder()
                .id(goods.getId())
                .name(goods.getName())
                .animationId(goods.getAnimation().getId())
                .animationTitle(goods.getAnimation().getTitle())
                .stores(stores)
                .build();
    }

    @Transactional(readOnly = false)
    public GoodsResponse createGoods(CreateGoodsRequest request) {
        // 중복된 이름의 굿즈가 있는지 확인
        if(goodsRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("이미 존재하는 상품입니다.");
        }

        // 애니메이션이 존재하는지 확인
        Animation animation = animationRepository.findById(request.animationId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 애니메이션입니다."));

        // Goods 엔티티 추가
        Goods goods = Goods.builder()
                .name(request.name())
                .animation(animation)
                .build();

        return GoodsResponse.from(goodsRepository.save(goods));
    }
}
