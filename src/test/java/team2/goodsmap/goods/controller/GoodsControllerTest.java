package team2.goodsmap.goods.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.global.jwt.JwtTokenProvider;
import team2.goodsmap.goods.dto.GoodsDetailResponse;
import team2.goodsmap.goods.dto.GoodsSimpleResponse;
import team2.goodsmap.goods.service.GoodsService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoodsController.class)
@AutoConfigureMockMvc(addFilters = false)
class GoodsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoodsService goodsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("GET /api/v1/goods?q= - 등록용 상품 목록을 조회한다")
    void getGoodsForRegistration_성공() throws Exception {
        // given
        given(goodsService.getGoodsForRegistration(any())).willReturn(List.of(
                GoodsSimpleResponse.builder()
                        .id(12L).name("마이멜로디 텀블러")
                        .animationId(5L).animationTitle("산리오 캐릭터즈")
                        .build()
        ));

        // when & then
        mockMvc.perform(get("/api/v1/goods").param("q", "마이멜로디"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("마이멜로디 텀블러"));
    }

    @Test
    @DisplayName("GET /api/v1/goods/search - 작품/지역/키워드로 탐색한다")
    void searchGoods_성공() throws Exception {
        // given
        given(goodsService.searchGoods(eq(5L), eq("서울"), any())).willReturn(List.of(
                GoodsSimpleResponse.builder()
                        .id(12L).name("마이멜로디 텀블러")
                        .animationId(5L).animationTitle("산리오 캐릭터즈")
                        .build()
        ));

        // when & then
        mockMvc.perform(get("/api/v1/goods/search")
                        .param("animationId", "5")
                        .param("region", "서울"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/goods/{goodsId} - 상세 조회 성공")
    void getGoodsDetail_성공() throws Exception {
        // given
        GoodsDetailResponse response = GoodsDetailResponse.builder()
                .id(12L).name("마이멜로디 텀블러")
                .animationId(5L).animationTitle("산리오 캐릭터즈")
                .stores(List.of())
                .build();
        given(goodsService.getGoodsDetail(12L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/goods/{goodsId}", 12L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("마이멜로디 텀블러"));
    }

    @Test
    @DisplayName("GET /api/v1/goods/{goodsId} - 존재하지 않으면 404를 반환한다")
    void getGoodsDetail_존재하지않으면_404() throws Exception {
        // given
        given(goodsService.getGoodsDetail(999L))
                .willThrow(new NotFoundException("존재하지 않는 상품입니다. id=999"));

        // when & then
        mockMvc.perform(get("/api/v1/goods/{goodsId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("존재하지 않는 상품입니다. id=999"));
    }
}
