package team2.goodsmap.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team2.goodsmap.store.entity.Store;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {

    @Query("""
        select s
        from Store s
        join StoreAdmin sa on sa.store = s
        where sa.user.id = :userId
    """)
    List<Store> findAllByAdminUserId(@Param("userId") Long userId);
}
