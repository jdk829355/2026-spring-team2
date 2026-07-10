package team2.goodsmap.global.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GeoUtils.distanceMeters(double lat1, double lng1, BigDecimal lat2, BigDecimal lng2)
 * Haversine 공식 검증용 순수 단위 테스트. Spring 컨텍스트/DB 불필요.
 */
class GeoUtilsTest {

    @Test
    void 같은_좌표면_거리는_0이다() {
        double distance = GeoUtils.distanceMeters(
                37.5, 127.0,
                BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0)
        );

        assertEquals(0.0, distance, 0.0001);
    }

    @Test
    void 서울역과_강남역_사이의_거리를_정확히_계산한다() {
        // 실제 좌표 기준 계산 결과 약 7944.32m (오차 1m 이내 허용)
        double distance = GeoUtils.distanceMeters(
                37.554648, 126.972559,
                BigDecimal.valueOf(37.498095), BigDecimal.valueOf(127.027610)
        );

        assertEquals(7944.32, distance, 1.0);
    }

    @Test
    void 두_지점_사이의_거리는_계산_방향에_관계없이_같다() {
        double aToB = GeoUtils.distanceMeters(
                37.554648, 126.972559,
                BigDecimal.valueOf(37.498095), BigDecimal.valueOf(127.027610)
        );
        double bToA = GeoUtils.distanceMeters(
                37.498095, 127.027610,
                BigDecimal.valueOf(37.554648), BigDecimal.valueOf(126.972559)
        );

        assertEquals(aToB, bToA, 0.0001);
    }

    @Test
    void 위도만_다르고_경도가_같으면_남북_방향_거리만_계산된다() {
        // 위도 1도 차이 ≈ 111km (지구 반지름 6,371,000m 기준)
        double distance = GeoUtils.distanceMeters(
                37.0, 127.0,
                BigDecimal.valueOf(38.0), BigDecimal.valueOf(127.0)
        );

        assertEquals(111_195.0, distance, 500.0);
    }
}
