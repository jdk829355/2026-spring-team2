package team2.goodsmap.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team2.goodsmap.store.entity.StoreAdmin;

import java.util.List;

public interface StoreAdminRepository extends JpaRepository<StoreAdmin, Long> {

    @Query("""
        select sa
        from StoreAdmin sa
        join fetch sa.store
        where sa.user.id = :userId
    """)
    List<StoreAdmin> findAllByUserId(@Param("userId") Long userId);
}
