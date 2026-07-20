package team2.goodsmap.goods.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.goods.dto.CreateGoodsRequest;
import team2.goodsmap.goods.dto.GoodsDetailResponse;
import team2.goodsmap.goods.dto.GoodsResponse;
import team2.goodsmap.goods.dto.GoodsSearchRow;
import team2.goodsmap.goods.dto.GoodsSimpleResponse;
import team2.goodsmap.goods.entity.Animation;
import team2.goodsmap.goods.entity.Goods;
import team2.goodsmap.goods.repository.AnimationRepository;
import team2.goodsmap.goods.repository.GoodsRepository;
import team2.goodsmap.store.entity.StoreGoods;
import team2.goodsmap.store.repository.StoreGoodsRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoodsService {

    private final GoodsRepository goodsRepository;
    private final StoreGoodsRepository storeGoodsRepository;
    private final AnimationRepository animationRepository;

    @Value("${aws.cdn.url}")
    private String cdnUrl;

    // 상품 목록 조회 (goods 테이블, 등록용) - GET /api/v1/goods?q
    public List<GoodsSimpleResponse> getGoodsForRegistration(String q) {
        String keyword = q == null || q.isBlank() ? null : q.trim();

        return goodsRepository.findGoodsForRegistration(keyword).stream()
                .map(goods -> GoodsSimpleResponse.builder()
                        .id(goods.goodsId())
                        .name(goods.goodsName())
                        .animationId(goods.animationId())
                        .animationTitle(goods.animationTitle())
                        .imageUrls(goods.imagePath() == null || goods.imagePath().isBlank()
                                ? List.of()
                                : List.of(toCdnUrl(goods.imagePath().trim())))
                        .build())
                .toList();
    }

    // 상품 목록 조회 (탐색용) - GET /api/v1/goods/search
    public List<GoodsSimpleResponse> searchGoods(Long animationId, String region, String keyword) {
        Map<Long, List<GoodsSearchRow>> rowsByGoodsId = goodsRepository
                .searchGoods(animationId, region, keyword)
                .stream()
                .collect(Collectors.groupingBy(
                        GoodsSearchRow::goodsId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return rowsByGoodsId.values().stream()
                .map(rows -> {
                    GoodsSearchRow goods = rows.getFirst();
                    List<String> imageUrls = rows.stream()
                            .map(GoodsSearchRow::imagePath)
                            .filter(imagePath -> imagePath != null && !imagePath.isBlank())
                            .map(this::toCdnUrl)
                            .distinct()
                            .toList();

                    return GoodsSimpleResponse.builder()
                            .id(goods.goodsId())
                            .name(goods.goodsName())
                            .animationId(goods.animationId())
                            .animationTitle(goods.animationTitle())
                            .imageUrls(imageUrls)
                            .build();
                })
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
                        .imagePath(toCdnUrl(sg.getImagePath()))
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

        Goods saved = goodsRepository.save(goods);
        log.atInfo()
                .addKeyValue("event", "GOODS_CREATED")
                .addKeyValue("goodsId", saved.getId())
                .addKeyValue("animationId", animation.getId())
                .log("상품 생성");

        return GoodsResponse.from(saved);
    }

    private String toCdnUrl(String imagePath) {
        return imagePath == null ? null : "https://" + cdnUrl + "/" + imagePath;
    }
}
