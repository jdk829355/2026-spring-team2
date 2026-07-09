package team2.goodsmap.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.entity.StoreAdmin;
import team2.goodsmap.user.entity.User;
import java.util.List;
import java.util.Optional;

public interface StoreAdminRepository extends JpaRepository<StoreAdmin, Long> {
    List<StoreAdmin> findAllByStore(Store store);

    Optional<StoreAdmin> findByIdAndStore(Long storeAdminId, Store store);

    boolean existsByUserAndStore(User user, Store store);
}
