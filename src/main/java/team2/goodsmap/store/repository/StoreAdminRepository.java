package team2.goodsmap.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.entity.StoreAdmin;
import team2.goodsmap.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface StoreAdminRepository extends JpaRepository<StoreAdmin, Long> {
    List<StoreAdmin> findAllByStore(Store store);

    Optional<StoreAdmin> findByIdAndStore(Long storeAdminId, Store store);

    boolean existsByUserAndStore(User user, Store store);

    Optional<StoreAdmin> findByStoreAndUser(Store store, User user);

    @Query(
            """
            SELECT EXISTS(SELECT 1 FROM StoreAdmin sa WHERE sa.user.id = :userId AND sa.store.id = :storeId)
            """
    )
    boolean existsByUserIdAndStoreId(Long userId, Long storeId);

    Long store(Store store);
}
