package team2.goodsmap.store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.global.location.dto.KakaoAddressSearchResponse;
import team2.goodsmap.global.location.service.KakaoGeocodingService;
import team2.goodsmap.global.util.GeoUtils;
import team2.goodsmap.goods.dto.GoodsResponse;
import team2.goodsmap.goods.repository.GoodsRepository;
import team2.goodsmap.store.dto.request.*;
import team2.goodsmap.store.dto.response.*;
import team2.goodsmap.store.dto.util.LatLng;
import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.entity.StoreAdmin;
import team2.goodsmap.store.entity.StoreGoods;
import team2.goodsmap.store.repository.StoreAdminRepository;
import team2.goodsmap.store.repository.StoreGoodsRepository;
import team2.goodsmap.store.repository.StoreRepository;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.enums.UserRole;
import team2.goodsmap.user.repository.UserRepository;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StoreService {

    //radius 파라미터 없을 때 기본 반경 (m)
    private static final double DEFAULT_RADIUS_METERS = 3_000.0;

    private final StoreRepository storeRepository;
    private final StoreAdminRepository storeAdminRepository;
    private final UserRepository userRepository;
    private final StoreGoodsRepository storeGoodsRepository;  //
    private final GoodsRepository goodsRepository;
    private final KakaoGeocodingService kakaoGeocodingService;


    public StoreResponse createStore(CreateStoreRequest request, Long userId) {
        validateCreateStoreRequest(request);

        User user = userRepository.findUserByIdAndRole(userId, UserRole.STORE).orElseThrow(
                () -> new IllegalArgumentException("사용자가 없습니다.")
        );

        LatLng latLng = addressToLatLng(request.address());

        Store store = Store.builder()
                .name(request.name())
                .description(request.description())
                .type(request.type())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .address(request.address())
                .lat(latLng.lat())
                .lng(latLng.lng())
                .build();

        StoreAdmin storeAdmin = StoreAdmin.builder()
                .user(user)
                .store(store)
                .build();

        storeRepository.save(store);
        storeAdminRepository.save(storeAdmin);
        log.info("[스토어생성] userId={}, storeId={}", userId, store.getId());
        return StoreResponse.from(store);
    }

    private void validateCreateStoreRequest(CreateStoreRequest request) {
        if (request.startDate() != null && request.endDate() != null && request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
    }

    public List<StoreResponse> getStoreByUserId(Long userId) {
        return storeRepository.findAllByAdminUserId(userId).stream()
                .map(StoreResponse::from)
                .toList();
    }

    public StoreAdminResponse createStoreAdmin(AddStoreAdminRequest request, Long storeId, Long userId){
        // 대상 사용자가 있는지
        User user = userRepository.findByEmail(request.email()).orElseThrow(
                () -> new IllegalArgumentException("사용자가 없습니다.")
        );

        // 업체가 있는지
        Store store = storeRepository.findById(storeId).orElseThrow(
                () -> new IllegalArgumentException("업체가 없습니다.")
        );

        // 추가를 요청한 관리자가 존재하는지 확인
        User adminUser = userRepository.findUserByIdAndRole(userId, UserRole.STORE).orElseThrow(
                () -> new IllegalArgumentException("관리자가 없습니다.")
        );

        // 추가를 요청한 관리자가 해당 업체의 관리자인지 확인
        if (!storeAdminRepository.existsByUserAndStore(adminUser, store)) {
            throw new IllegalArgumentException("해당 업체의 관리자가 아닙니다.");
        }

        if(storeAdminRepository.existsByUserAndStore(user, store)) {
            throw new IllegalArgumentException("이미 등록된 관리자입니다.");
        }

        StoreAdmin storeAdmin = StoreAdmin.builder()
                .user(user)
                .store(store)
                .build();

        storeAdminRepository.save(storeAdmin);
        log.info("[관리자추가] actorUserId={}, storeId={}, newAdminId={}",
                userId, storeId, user.getId());
        return StoreAdminResponse.from(storeAdmin);
    }

    public List<StoreAdminResponse> getStoreAdmin(Long storeId, Long userId) {
        Store store = storeRepository.findById(storeId).orElseThrow(
                () -> new IllegalArgumentException("업체가 없습니다.")
        );

        // 실제 사용자 맞나
        User user = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("사용자가 없습니다.")
        );

        // 조회하는 사람이 해당 업체의 관리자인가?
        storeAdminRepository.findByStoreAndUser(store, user).orElseThrow(
                () -> new IllegalArgumentException("해당 업체의 관리자가 아닙니다.")
        );

        return storeAdminRepository.findAllByStore(store).stream()
                .map(StoreAdminResponse::from)
                .toList();
    }

    public void deleteStoreAdmin(Long userId, Long storeId, Long storeAdminId) {
        User actor = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("사용자가 없습니다.")
        );

        if (actor.getRole() != UserRole.STORE) {
            throw new IllegalArgumentException("관리 권한이 없습니다.");
        }

        Store store = storeRepository.findById(storeId).orElseThrow(
                () -> new IllegalArgumentException("업체가 없습니다.")
        );

        if (!storeAdminRepository.existsByUserAndStore(actor, store)) {
            throw new IllegalArgumentException("해당 업체의 관리 권한이 없습니다.");
        }

        StoreAdmin target = storeAdminRepository.findByIdAndStore(storeAdminId, store).orElseThrow(
                () -> new IllegalArgumentException("해당하는 관리자가 없습니다.")
        );

        if (target.getUser().getRole() != UserRole.USER) {
            throw new IllegalArgumentException("해당 사용자는 삭제 대상이 아닙니다.");
        }

        storeAdminRepository.delete(target);
        log.info("[관리자삭제] actorUserId={}, storeId={}, storeAdminId={}",
                userId, storeId, storeAdminId);
    }

    // 유저가 해당 업체에 관리 권한이 있는지 체크
    public void validateStoreAdmin(Long userId, Long storeId, String message) {
        if (!storeAdminRepository.existsByUserIdAndStoreId(userId, storeId)) {
            throw new IllegalArgumentException(message);
        }
    }

    public void validateStoreGoods(Long storeId, Long storeGoodsId){
        if (!storeGoodsRepository.existsByStoreIdAndStoreGoodsId(storeId, storeGoodsId)) {
            throw new IllegalArgumentException("해당 상품이 없습니다.");
        }
    }

    // StoreGoods를 생성 (Goods도 새로 만든 경우) - POST /api/v1/stores/{storeId}/goods/new
    public StoreGoodsResponse createStoreGoods (AddNewStoreGoodsRequest request, GoodsResponse goodsResponse, Long userId, Long storeId) {
        // 업체 관리자 여부 확인
        validateStoreAdmin(userId, storeId, "상품 추가 권한이 없습니다.");
        // StoreGoods 생성
        StoreGoods storeGoods = StoreGoods.builder()
                .price(request.price())
                .stock(request.stock())
                .goods(goodsRepository.findById(goodsResponse.id()).orElseThrow(() -> new IllegalArgumentException("상품이 없습니다.")))
                .store(storeRepository.findById(storeId).orElseThrow(() -> new IllegalArgumentException("업체가 없습니다.")))
                .build();

        storeGoodsRepository.save(storeGoods);
        log.info("[재고등록:신규상품] userId={}, storeId={}, storeGoodsId={}",
                userId, storeId, storeGoods.getId());
        // StoreGoodsResponse 반환
        return StoreGoodsResponse.from(storeGoods);
    }

    // StoreGoods를 생성 (Goods가 기존에 있는 경우) - POST /api/v1/stores/{storeId}/goods
    public StoreGoodsResponse createStoreGoods (AddExistingStoreGoodsRequest request, Long userId, Long storeId) {
        // 업체 관리자 여부 확인
        validateStoreAdmin(userId, storeId, "상품 추가 권한이 없습니다.");
        // StoreGoods 생성
        StoreGoods storeGoods = StoreGoods.builder()
                .price(request.price())
                .stock(request.stock())
                .goods(goodsRepository.findById(request.goodsId()).orElseThrow(() -> new IllegalArgumentException("상품이 없습니다.")))
                .store(storeRepository.findById(storeId).orElseThrow(() -> new IllegalArgumentException("업체가 없습니다.")))
                .build();

        storeGoodsRepository.save(storeGoods);
        log.info("[재고등록:기존상품] userId={}, storeId={}, storeGoodsId={}, goodsId={}",
                userId, storeId, storeGoods.getId(), request.goodsId());
        // StoreGoodsResponse 반환
        return StoreGoodsResponse.from(storeGoods);
    }

    // 재고, 가격, 이미지 경로 수정 - PATCH /api/v1/stores/{storeId}/goods/{storeGoodsId}
    public StoreGoodsResponse modifyStoreGoods(Long storeId, Long storeGoodsId, UpdateStoreGoodsRequest request, Long userId) {
        // 유저 권한 확인
        validateStoreAdmin(userId, storeId, "수정 권한이 없습니다.");
        validateStoreGoods(storeId, storeGoodsId);

        StoreGoods storeGoods = storeGoodsRepository.findWithGoodsById(storeGoodsId).orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다."));

        // 수정 로직
        storeGoods.update(request.price(), request.stock(), request.imagePath());

        storeGoodsRepository.save(storeGoods);
        log.info("[재고수정] userId={}, storeId={}, storeGoodsId={}", userId, storeId, storeGoodsId);
        return StoreGoodsResponse.from(storeGoods);
    }

    public void deleteStoreGoods(Long storeId, Long storeGoodsId, Long userId) {
        // 유저 권한 확인
        validateStoreAdmin(userId, storeId, "삭제 권한이 없습니다.");
        validateStoreGoods(storeId, storeGoodsId);

        StoreGoods storeGoods = storeGoodsRepository.findWithGoodsById(storeGoodsId).orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다."));
        storeGoodsRepository.delete(storeGoods);
        log.info("[재고삭제] userId={}, storeId={}, storeGoodsId={}", userId, storeId, storeGoodsId);
    }

    public StoreResponse updateStore(UpdateStoreRequest request, Long storeId, Long userId){
        // 유저 권한 확인
        validateStoreAdmin(userId, storeId, "수정 권한이 없습니다.");
        // Store 업데이트 로직
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new IllegalArgumentException("해당 스토어가 없습니다."));

        LatLng latLng = null;
        if(request.address() != null) {
            latLng = addressToLatLng(request.address());
        }

        store.update(request.name(), request.description(), request.type(), request.startDate(), request.endDate(), request.address(), latLng != null ? latLng.lat() : null, latLng != null ? latLng.lng() : null);

        storeRepository.save(store);
        log.info("[스토어수정] userId={}, storeId={}", userId, storeId);
        return StoreResponse.from(store);
    }

    public StoreDetailResponse getStoreDetail(Long storeId, Long userId) {
        // store 존재 확인
        if(!storeRepository.existsById(storeId)) {
            throw new NotFoundException("존재하지 않는 스토어입니다. id=" + storeId);
        }
        validateStoreAdmin(userId, storeId, "상세 정보 조회 권한이 없습니다.");

        return storeRepository.getStoreDetail(storeId);
    }

    public void updateImagePath(Long userId, Long storeId, Long storeGoodsId, AddImagePathRequest request) {
        validateStoreAdmin(userId, storeId, "이미지 경로 수정 권한이 없습니다.");
        validateStoreGoods(storeId, storeGoodsId);

        StoreGoods storeGoods = storeGoodsRepository.findById(storeGoodsId).orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다."));
        storeGoods.updateImagePath(request.imagePath());
        storeGoodsRepository.save(storeGoods);
        log.info("[이미지경로수정] userId={}, storeId={}, storeGoodsId={}", userId, storeId, storeGoodsId);
    }

    private LatLng addressToLatLng(String addr){
        List<KakaoAddressSearchResponse.Document> addressSearchResponse = kakaoGeocodingService.searchAddress(addr).documents();
        if (addressSearchResponse.isEmpty()) {
            throw new IllegalArgumentException("주소를 찾을 수 없습니다.");
        }
        return LatLng.from(addressSearchResponse.getFirst());
    }

    // 재고 조회(Public) 3개 메서드

    // 스토어 목록 조회 - GET /api/v1/stores
    public List<StoreResponse> getStores(Long animationId, String region, String keyword) {
        return storeRepository.searchStores(animationId, region, keyword).stream()
                .map(StoreResponse::from)
                .toList();
    }

    // 스토어 목록 조회(지도용) - GET /api/v1/stores/map
    public List<StoreMapResponse> getStoresForMap(double lat, double lng, Double radius,
                                                   Long animationId, String region) {
        double searchRadius = (radius == null) ? DEFAULT_RADIUS_METERS : radius;

        return storeRepository.findAllForMap(animationId, region).stream()
                .filter(store -> store.getLat() != null && store.getLng() != null)
                .map(store -> {
                    double distance = GeoUtils.distanceMeters(lat, lng, store.getLat(), store.getLng());
                    return new StoreMapResponse(
                            store.getId(), store.getName(), store.getType().name(), store.getAddress(),
                            store.getLat(), store.getLng(), store.getStartDate(), store.getEndDate(),
                            distance);
                })
                .filter(r -> r.distance() <= searchRadius)
                .sorted(Comparator.comparingDouble(StoreMapResponse::distance))
                .toList();
    }

    // 매장별 전체 재고 목록 조회 - GET /api/v1/stores/{storeId}/goods
    public List<StoreGoodsItemResponse> getStoreGoods(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new NotFoundException("존재하지 않는 스토어입니다. id=" + storeId);
        }

        return storeGoodsRepository.findByStoreId(storeId).stream()
                .map(sg -> new StoreGoodsItemResponse(
                        sg.getId(), sg.getGoods().getId(), sg.getGoods().getName(),
                        sg.getGoods().getAnimation().getTitle(), sg.getPrice(), sg.getStock(), sg.getImagePath()))
                .toList();
    }
}