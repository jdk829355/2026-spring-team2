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
        List<StoreAdmin> storeAdmins = storeAdminRepository.findAllByUserId(userId);
        if (storeAdmins.isEmpty()) {
            throw new IllegalArgumentException("해당 사용자가 관리하는 가게가 없습니다.");
        }
        return storeAdmins.stream()
                .map(StoreAdmin::getStore)
                .map(StoreResponse::from)
                .toList();
    }
}
