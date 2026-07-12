package team2.goodsmap.global.location.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import team2.goodsmap.global.location.dto.KakaoAddressSearchResponse;
import team2.goodsmap.global.location.util.KoreanRoadAddressNormalizer;

@Service
@RequiredArgsConstructor
public class KakaoGeocodingService {
    private final RestClient kakaoLocalRestClient;


    public KakaoAddressSearchResponse searchAddress(String query) {
        KoreanRoadAddressNormalizer.NormalizedAddress normalized = KoreanRoadAddressNormalizer.normalize(query);
        if(!normalized.roadAddressMatched()) {
            throw new IllegalArgumentException("유효한 도로명 주소를 입력해주세요.");
        }
        query = normalized.searchAddress();
        String finalQuery = query;
        return kakaoLocalRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/address.json")
                        .queryParam("query", finalQuery)
                        .build()
                )
                .retrieve()
                .body(KakaoAddressSearchResponse.class);
    }
}

