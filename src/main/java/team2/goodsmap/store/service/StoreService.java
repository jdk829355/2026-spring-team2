package team2.goodsmap.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team2.goodsmap.store.dto.request.AddStoreAdminRequest;
import team2.goodsmap.store.dto.request.CreateStoreRequest;
import team2.goodsmap.store.dto.response.StoreAdminResponse;
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

    public StoreAdminResponse createStoreAdmin(AddStoreAdminRequest request, Long storeId){
        User user = userRepository.findUserByEmail(request.email()).orElseThrow(
                () -> new IllegalArgumentException("사용자가 없습니다.")
        );

        Store store = storeRepository.findById(storeId).orElseThrow(
                () -> new IllegalArgumentException("업체가 없습니다.")
        );

        StoreAdmin storeAdmin = StoreAdmin.builder()
                .user(user)
                .store(store)
                .build();

        storeAdminRepository.save(storeAdmin);
        return StoreAdminResponse.from(storeAdmin);
    }


    public List<StoreAdminResponse> getStoreAdmin(Long storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(
                () -> new IllegalArgumentException("업체가 없습니다.")
        );

        return storeAdminRepository.findAllByStore(store).stream()
                .map(StoreAdminResponse::from)
                .toList();
    }

    public void deleteStoreAdmin(Long userId, Long storeId, Long storeAdminId) {
        // userId에 해당하는 유저는 삭제하는 유저 (storeAdmin에 속해있으면서 role이 STORE여야함)
        User actor = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("사용자가 없습니다.")
        );

        if (actor.getRole() != UserRole.STORE) {
            throw new IllegalArgumentException("관리 권한이 없습니다.");
        }

        Store store = storeRepository.findById(storeId).orElseThrow(
                () -> new IllegalArgumentException("업체가 없습니다.")
        );

        // 삭제 요청자가 해당 store의 관리자인지 확인
        if (!storeAdminRepository.existsByUserAndStore(actor, store)) {
            throw new IllegalArgumentException("해당 업체의 관리 권한이 없습니다.");
        }

        // storeAdminId에 해당하는 유저는 USER role이면서 storeAdmin에 속해있어야함.
        StoreAdmin target = storeAdminRepository.findByIdAndStore(storeAdminId, store).orElseThrow(
                () -> new IllegalArgumentException("해당하는 관리자가 없습니다.")
        );

        if (target.getUser().getRole() != UserRole.USER) {
            throw new IllegalArgumentException("해당 사용자는 삭제 대상이 아닙니다.");
        }

        storeAdminRepository.delete(target);
    }

}
