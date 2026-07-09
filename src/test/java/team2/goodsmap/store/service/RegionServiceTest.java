package team2.goodsmap.store.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team2.goodsmap.store.repository.StoreRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private RegionService regionService;

    @Test
    @DisplayName("주소에서 지역명(첫 단어)을 추출하고 중복을 제거해 정렬해서 반환한다")
    void getRegions_추출및중복제거() {
        // given
        given(storeRepository.findAllAddresses()).willReturn(List.of(
                "서울특별시 강남구 테헤란로 123",
                "서울특별시 마포구 양화로 45",
                "부산광역시 해운대구 센텀중앙로 55"
        ));

        // when
        List<String> result = regionService.getRegions();

        // then
        assertThat(result).containsExactly("부산광역시", "서울특별시");
    }

    @Test
    @DisplayName("주소가 비어있거나 null이면 결과에서 제외한다")
    void getRegions_빈주소_제외() {
        // given
        given(storeRepository.findAllAddresses()).willReturn(java.util.Arrays.asList(
                "서울특별시 강남구", "", null, "  "
        ));

        // when
        List<String> result = regionService.getRegions();

        // then
        assertThat(result).containsExactly("서울특별시");
    }
}
