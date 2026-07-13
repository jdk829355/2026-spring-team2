package team2.goodsmap.global.location.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import team2.goodsmap.global.location.dto.KakaoAddressSearchResponse;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KakaoGeocodingServiceTest {

    private RestClient restClient;
    private KakaoGeocodingService service;

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        service = new KakaoGeocodingService(restClient);
    }

    @Test
    @DisplayName("유효한 도로명 주소를 입력하면 위경도 응답을 반환한다")
    void searchAddress_도로명_주소_정상_검색() {
        // given
        KakaoAddressSearchResponse expected = new KakaoAddressSearchResponse(List.of(
                new KakaoAddressSearchResponse.Document(
                        "서울특별시 마포구 양화로 188",
                        "126.926487",
                        "37.557743"
                )
        ));

        when(restClient.get()
                .uri(any(java.util.function.Function.class))
                .retrieve()
                .body(KakaoAddressSearchResponse.class))
                .thenReturn(expected);

        // when
        KakaoAddressSearchResponse result = service.searchAddress("서울특별시 마포구 양화로 188");

        // then
        assertThat(result.documents()).hasSize(1);
        KakaoAddressSearchResponse.Document doc = result.documents().get(0);
        assertThat(doc.addressName()).isEqualTo("서울특별시 마포구 양화로 188");
        assertThat(doc.x()).isEqualTo("126.926487");
        assertThat(doc.y()).isEqualTo("37.557743");
    }

    @Test
    @DisplayName("도로명 주소가 아니면 IllegalArgumentException이 발생한다")
    void searchAddress_도로명_주소가_아니면_예외() {
        assertThatThrownBy(() -> service.searchAddress("서울시 강남구"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효한 도로명 주소를 입력해주세요.");
    }

    @Test
    @DisplayName("null 또는 빈 문자열이면 IllegalArgumentException이 발생한다")
    void searchAddress_빈_주소_예외() {
        assertThatThrownBy(() -> service.searchAddress(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주소는 비어 있을 수 없습니다.");

        assertThatThrownBy(() -> service.searchAddress("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("주소는 비어 있을 수 없습니다.");
    }

    @Test
    @DisplayName("지번 주소는 도로명이 아니므로 예외가 발생한다")
    void searchAddress_지번_주소_예외() {
        assertThatThrownBy(() -> service.searchAddress("서울특별시 마포구 서교동 123-45"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효한 도로명 주소를 입력해주세요.");
    }

    @Test
    @DisplayName("도로명 주소 약어 매핑이 정상 동작한다 (예: 서울 → 서울특별시)")
    void searchAddress_지역명_약어_정규화() {
        // given
        KakaoAddressSearchResponse expected = new KakaoAddressSearchResponse(List.of(
                new KakaoAddressSearchResponse.Document(
                        "서울특별시 강남구 테헤란로 123",
                        "127.123",
                        "37.456"
                )
        ));

        when(restClient.get()
                .uri(any(java.util.function.Function.class))
                .retrieve()
                .body(KakaoAddressSearchResponse.class))
                .thenReturn(expected);

        // when: "서울 강남구 테헤란로 123" → "서울특별시 강남구 테헤란로 123" 정규화
        KakaoAddressSearchResponse result = service.searchAddress("서울 강남구 테헤란로 123");

        // then
        assertThat(result.documents()).hasSize(1);
        assertThat(result.documents().get(0).y()).isEqualTo("37.456");
        assertThat(result.documents().get(0).x()).isEqualTo("127.123");
    }

    @Test
    @DisplayName("Kakao API 응답에 documents가 비어 있어도 정상 반환한다 (빈 리스트)")
    void searchAddress_빈_검색_결과() {
        // given
        KakaoAddressSearchResponse emptyResponse = new KakaoAddressSearchResponse(Collections.emptyList());

        when(restClient.get()
                .uri(any(java.util.function.Function.class))
                .retrieve()
                .body(KakaoAddressSearchResponse.class))
                .thenReturn(emptyResponse);

        // when
        KakaoAddressSearchResponse result = service.searchAddress("경기도 용인시 기흥구 존재하지않는길 999");

        // then
        assertThat(result.documents()).isEmpty();
    }

    @Test
    @DisplayName("상세 주소가 포함된 도로명 주소도 정상 처리된다")
    void searchAddress_상세주소_포함() {
        // given
        KakaoAddressSearchResponse expected = new KakaoAddressSearchResponse(List.of(
                new KakaoAddressSearchResponse.Document(
                        "경기도 용인시 기흥구 덕영대로 1732",
                        "127.111",
                        "37.222"
                )
        ));

        when(restClient.get()
                .uri(any(java.util.function.Function.class))
                .retrieve()
                .body(KakaoAddressSearchResponse.class))
                .thenReturn(expected);

        // when: 상세주소(101호) 포함 → 검색 쿼리에서는 제거되고 "경기도 용인시 기흥구 덕영대로 1732"로 검색
        KakaoAddressSearchResponse result = service.searchAddress("경기도 용인시 기흥구 덕영대로 1732 우정원 101호");

        // then
        assertThat(result.documents()).hasSize(1);
        assertThat(result.documents().get(0).addressName()).isEqualTo("경기도 용인시 기흥구 덕영대로 1732");
    }
}
