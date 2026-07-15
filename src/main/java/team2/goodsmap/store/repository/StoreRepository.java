package team2.goodsmap.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team2.goodsmap.store.dto.response.StoreDetailResponse;
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
    // 스토어 목록 조회 - 작품/지역/키워드 필터링
    @Query("""
        SELECT DISTINCT s FROM Store s
        WHERE (:region IS NULL OR s.address LIKE CONCAT('%', CAST(:region AS string), '%'))
          AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
          AND (:animationId IS NULL OR s.id IN (
                SELECT sg.store.id FROM StoreGoods sg
                WHERE sg.goods.animation.id = :animationId
          ))
        """)
    List<Store> searchStores(@Param("animationId") Long animationId,
                             @Param("region") String region,
                             @Param("keyword") String keyword);

    // 스토어 목록 조회 (지도용) - 반경 필터는 서비스단에서 처리, 여기선 작품/지역만 1차 필터
    @Query("""
        SELECT s FROM Store s
        WHERE (:region IS NULL OR s.address LIKE CONCAT('%', CAST(:region AS string), '%'))
          AND (:animationId IS NULL OR s.id IN (
                SELECT sg.store.id FROM StoreGoods sg
                WHERE sg.goods.animation.id = :animationId
          ))
        """)
    List<Store> findAllForMap(@Param("animationId") Long animationId,
                              @Param("region") String region);

    // 지역 탭 목록 조회용 - 전체 주소 원본
    @Query("SELECT DISTINCT s.address FROM Store s")
    List<String> findAllAddresses();

    @Query("""
        SELECT
            s.id,
            s.name,
            s.description,
            s.type,
            s.startDate,
            s.endDate,
            s.address,
            s.lat,
            s.lng,
            COUNT(DISTINCT sg.goods) AS goodsCount
        FROM Store s
        LEFT JOIN StoreGoods sg ON sg.store = s
        WHERE s.id = :storeId
        GROUP BY s.id
    """)
    StoreDetailResponse getStoreDetail(Long storeId);
}
