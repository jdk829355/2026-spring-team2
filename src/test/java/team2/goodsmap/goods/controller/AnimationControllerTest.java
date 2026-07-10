package team2.goodsmap.goods.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import team2.goodsmap.global.jwt.JwtTokenProvider;
import team2.goodsmap.goods.dto.AnimationResponse;
import team2.goodsmap.goods.service.AnimationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 재고 도메인은 비로그인 상태(Public)에서도 조회 가능해야 하므로
 * addFilters = false로 시큐리티 필터 체인을 우회해 검증한다.
 * (SecurityConfig 자체의 permitAll 설정은 별도로 확인 필요 - 이 테스트는 컨트롤러 동작만 검증)
 */
@WebMvcTest(AnimationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnimationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimationService animationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("GET /api/v1/animations - 작품 목록을 정상 조회한다")
    void getAnimations_성공() throws Exception {
        // given
        given(animationService.getAnimations(any())).willReturn(List.of(
                AnimationResponse.builder().id(1L).title("산리오 캐릭터즈").build(),
                AnimationResponse.builder().id(2L).title("짱구는 못말려").build()
        ));

        // when & then
        mockMvc.perform(get("/api/v1/animations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("산리오 캐릭터즈"));
    }

    @Test
    @DisplayName("GET /api/v1/animations?keyword= - 검색어로 조회한다")
    void getAnimations_키워드검색() throws Exception {
        // given
        given(animationService.getAnimations("산리오")).willReturn(List.of(
                AnimationResponse.builder().id(1L).title("산리오 캐릭터즈").build()
        ));

        // when & then
        mockMvc.perform(get("/api/v1/animations").param("keyword", "산리오"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
