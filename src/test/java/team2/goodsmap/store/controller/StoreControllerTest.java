package team2.goodsmap.store.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.store.dto.StoreGoodsItemResponse;
import team2.goodsmap.store.dto.StoreMapResponse;
import team2.goodsmap.store.dto.StoreResponse;
import team2.goodsmap.store.service.StoreService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StoreController.class)
@AutoConfigureMockMvc(addFilters = false)
class StoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreService storeService;

    @Test
    @DisplayName("GET /api/v1/stores - 스토어 목록을 조회한다")
    void getStores_성공() throws Exception {
        // given
        given(storeService.getStores(any(), any(), any())).willReturn(List.of(
                StoreResponse.builder().id(1L).name("강남점").type("POPUP").address("서울특별시 강남구").build()
        ));

        // when & then
        mockMvc.perform(get("/api/v1/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("강남점"));
    }

    @Test
    @DisplayName("GET /api/v1/stores/map - lat, lng 없이 요청하면 400을 반환한다")
    void getStoresForMap_필수파라미터_누락시_400() throws Exception {
        mockMvc.perform(get("/api/v1/stores/map"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/stores/map - lat, lng로 지도용 목록을 조회한다")
    void getStoresForMap_성공() throws Exception {
        // given
        given(storeService.getStoresForMap(anyDouble(), anyDouble(), any(), any(), any()))
                .willReturn(List.of(
                        StoreMapResponse.builder().id(1L).name("강남점").type("POPUP")
                                .address("서울특별시 강남구").distance(120.5).build()
                ));

        // when & then
        mockMvc.perform(get("/api/v1/stores/map")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("강남점"))
                .andExpect(jsonPath("$.data[0].distance").value(120.5));
    }

    @Test
    @DisplayName("GET /api/v1/stores/{storeId}/goods - 매장별 재고 목록을 조회한다")
    void getStoreGoods_성공() throws Exception {
        // given
        given(storeService.getStoreGoods(7L)).willReturn(List.of(
                StoreGoodsItemResponse.builder()
                        .storeGoodsId(58L).goodsId(12L).goodsName("마이멜로디 텀블러")
                        .animationTitle("산리오 캐릭터즈").price(15000).stock(30).imagePath("img.png")
                        .build()
        ));

        // when & then
        mockMvc.perform(get("/api/v1/stores/{storeId}/goods", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].goodsName").value("마이멜로디 텀블러"));
    }

    @Test
    @DisplayName("GET /api/v1/stores/{storeId}/goods - 존재하지 않는 스토어면 404를 반환한다")
    void getStoreGoods_존재하지않으면_404() throws Exception {
        // given
        given(storeService.getStoreGoods(999L))
                .willThrow(new NotFoundException("존재하지 않는 스토어입니다. id=999"));

        // when & then
        mockMvc.perform(get("/api/v1/stores/{storeId}/goods", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
