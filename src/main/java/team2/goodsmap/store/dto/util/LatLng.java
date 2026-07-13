package team2.goodsmap.store.dto.util;

import team2.goodsmap.global.location.dto.KakaoAddressSearchResponse;

import java.math.BigDecimal;

public record LatLng(
        BigDecimal lat,
        BigDecimal lng
) {
    public static LatLng from(KakaoAddressSearchResponse.Document document) {
        return new LatLng(
                new BigDecimal(document.y()),
                new BigDecimal(document.x())
        );
    }
}
