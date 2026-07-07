package team2.goodsmap.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team2.goodsmap.store.entity.Store;

public interface StoreRepository extends JpaRepository<Store, Long> {
}
