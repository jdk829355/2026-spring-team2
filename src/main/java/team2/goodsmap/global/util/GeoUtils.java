package team2.goodsmap.global.util;

import java.math.BigDecimal;

public class GeoUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private GeoUtils() {
    }

    /**
     * 두 좌표 사이의 거리를 미터(m) 단위로 계산한다. (Haversine 공식)
     */
    public static double distanceMeters(double lat1, double lng1, BigDecimal lat2, BigDecimal lng2) {
        double dLat = Math.toRadians(lat2.doubleValue() - lat1);
        double dLng = Math.toRadians(lng2.doubleValue() - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}
