package team2.goodsmap.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.store.dto.request.CreateStoreRequest;
import team2.goodsmap.store.dto.response.StoreResponse;
import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.entity.StoreAdmin;
import team2.goodsmap.store.repository.StoreAdminRepository;
import team2.goodsmap.store.repository.StoreRepository;
import team2.goodsmap.user.entity.User;
import team2.goodsmap.user.enums.UserRole;
import team2.goodsmap.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreService {
    private final StoreRepository storeRepository;
    private final StoreAdminRepository storeAdminRepository;
    private final UserRepository userRepository;

    public StoreResponse createStore(CreateStoreRequest request, Long userId) {
        validateCreateStoreRequest(request);

        User user = userRepository.findUserByIdAndRole(userId, UserRole.STORE).orElseThrow(
                () -> new IllegalArgumentException("사용자가 없습니다.")
        );

        Store store = Store.builder()
                .name(request.name())
                .description(request.description())
                .type(request.type())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .address(request.address())
                .lat(request.lat())
                .lng(request.lng())
                .build();

        StoreAdmin storeAdmin = StoreAdmin.builder()
                .user(user)
                .store(store)
                .build();

        storeRepository.save(store);
        storeAdminRepository.save(storeAdmin);
        return StoreResponse.from(store);
    }

    public List<StoreResponse> getStoreByUserId(Long userId) {
        return storeRepository.findAllByAdminUserId(userId).stream()
                .map(StoreResponse::from)
                .toList();
    }

    private void validateCreateStoreRequest(CreateStoreRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("요청 값이 없습니다.");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("업체명은 필수입니다.");
        }
        if (request.type() == null) {
            throw new IllegalArgumentException("업체 타입은 필수입니다.");
        }
        if (request.address() == null || request.address().isBlank()) {
            throw new IllegalArgumentException("주소는 필수입니다.");
        }
        if (request.lat() == null || request.lng() == null) {
            throw new IllegalArgumentException("위도와 경도는 필수입니다.");
        }
        if (request.startDate() != null && request.endDate() != null && request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
    }
}
