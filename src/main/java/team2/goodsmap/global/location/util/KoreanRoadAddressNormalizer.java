package team2.goodsmap.global.location.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KoreanRoadAddressNormalizer {

    private KoreanRoadAddressNormalizer() {
    }

    /**
     * 도로명과 건물번호까지 추출합니다.
     * 다만, 약한 검증이며 클라이언트에서 별도의 도로명 주소 검색 api를 사용해야할 것 같습니다.
     *
     * 예:
     * 경기도 용인시 기흥구 덕영대로 1732 우정원 101호
     * → 경기도 용인시 기흥구 덕영대로 1732
     */
    private static final Pattern ROAD_ADDRESS_PATTERN = Pattern.compile(
            "^(.*?)" +
                    "([가-힣A-Za-z0-9·.\\-]+(?:대로|로|길))" +
                    "\\s*" +
                    "(\\d+(?:-\\d+)?)" +
                    "(?:\\s+(.*))?$"
    );

    private static final Pattern MULTIPLE_SPACES =
            Pattern.compile("\\s+");

    private static final Pattern SEPARATOR =
            Pattern.compile("[,;|]+");

    private static final Pattern PARENTHESES =
            Pattern.compile("\\([^)]*\\)");

    private static final Map<String, String> REGION_ALIASES = Map.ofEntries(
            Map.entry("서울 ", "서울특별시 "),
            Map.entry("부산 ", "부산광역시 "),
            Map.entry("대구 ", "대구광역시 "),
            Map.entry("인천 ", "인천광역시 "),
            Map.entry("광주 ", "광주광역시 "),
            Map.entry("대전 ", "대전광역시 "),
            Map.entry("울산 ", "울산광역시 "),
            Map.entry("세종 ", "세종특별자치시 "),
            Map.entry("경기 ", "경기도 "),
            Map.entry("강원 ", "강원특별자치도 "),
            Map.entry("충북 ", "충청북도 "),
            Map.entry("충남 ", "충청남도 "),
            Map.entry("전북 ", "전북특별자치도 "),
            Map.entry("전남 ", "전라남도 "),
            Map.entry("경북 ", "경상북도 "),
            Map.entry("경남 ", "경상남도 "),
            Map.entry("제주 ", "제주특별자치도 ")
    );

    public static NormalizedAddress normalize(String rawAddress) {
        if (rawAddress == null || rawAddress.isBlank()) {
            throw new IllegalArgumentException("주소는 비어 있을 수 없습니다.");
        }

        String normalized = rawAddress.strip();

        // 줄바꿈, 탭, 연속 공백 정리
        normalized = MULTIPLE_SPACES.matcher(normalized).replaceAll(" ");

        // 괄호 안 참고 정보 제거
        // 예: 서울시 강남구 테헤란로 123 (역삼동)
        normalized = PARENTHESES.matcher(normalized).replaceAll(" ");

        // 쉼표, 세미콜론 등을 공백으로 변경
        normalized = SEPARATOR.matcher(normalized).replaceAll(" ");

        normalized = MULTIPLE_SPACES.matcher(normalized)
                .replaceAll(" ")
                .strip();

        normalized = normalizeRegionName(normalized);

        Matcher matcher = ROAD_ADDRESS_PATTERN.matcher(normalized);

        if (!matcher.matches()) {
            return new NormalizedAddress(
                    normalized,
                    null,
                    false
            );
        }

        String regionPart = matcher.group(1).strip();
        String roadName = matcher.group(2).strip();
        String buildingNumber = matcher.group(3).strip();
        String detailAddress = matcher.group(4);

        String searchAddress = String.join(
                " ",
                regionPart,
                roadName,
                buildingNumber
        ).replaceAll("\\s+", " ").strip();

        if (detailAddress != null) {
            detailAddress = detailAddress.strip();

            if (detailAddress.isBlank()) {
                detailAddress = null;
            }
        }

        return new NormalizedAddress(
                searchAddress,
                detailAddress,
                true
        );
    }

    private static String normalizeRegionName(String address) {
        for (Map.Entry<String, String> entry : REGION_ALIASES.entrySet()) {
            if (address.startsWith(entry.getKey())) {
                return entry.getValue()
                        + address.substring(entry.getKey().length());
            }
        }

        return address;
    }

    public record NormalizedAddress(
            String searchAddress,
            String detailAddress,
            boolean roadAddressMatched
    ) {
    }
}