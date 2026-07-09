package team2.goodsmap.planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import team2.goodsmap.global.exception.BadRequestException;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.planner.dto.PlannerGoodsCreateRequest;
import team2.goodsmap.planner.dto.PlannerGoodsCreateResponse;
import team2.goodsmap.planner.service.PlannerGoodsService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spring-security-test 의존성 필요 (testImplementation 'org.springframework.security:spring-security-test')
 * addFilters=false로 실제 JwtAuthenticationFilter는 타지 않지만,
 * .with(authentication(...))으로 SecurityContext에 인증 정보를 직접 넣어
 * @AuthenticationPrincipal 동작만 독립적으로 검증한다.
 */
@WebMvcTest(PlannerGoodsController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlannerGoodsControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlannerGoodsService plannerGoodsService;

    private UsernamePasswordAuthenticationToken loginAs(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("POST /api/v1/planner-goods - 로그인 유저가 정상적으로 살 것을 담는다")
    void createPlannerGoods_성공() throws Exception {
        // given
        PlannerGoodsCreateRequest request = new PlannerGoodsCreateRequest(null, "2026-07-15", 58L);
        PlannerGoodsCreateResponse response = PlannerGoodsCreateResponse.builder()
                .plannerId(21L).plannerGoodsId(103L).build();

        given(plannerGoodsService.addPlannerGoods(eq(USER_ID), any(PlannerGoodsCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/planner-goods")
                        .with(authentication(loginAs(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.plannerId").value(21))
                .andExpect(jsonPath("$.data.plannerGoodsId").value(103));
    }

    @Test
    @DisplayName("storeGoodsId가 없으면 400을 반환한다 (@Valid 검증)")
    void createPlannerGoods_storeGoodsId_누락시_400() throws Exception {
        // given: storeGoodsId 없이 요청
        String invalidBody = """
                {"date": "2026-07-15"}
                """;

        // when & then
        mockMvc.perform(post("/api/v1/planner-goods")
                        .with(authentication(loginAs(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("plannerId, date가 둘 다 없으면 서비스에서 400을 반환한다")
    void createPlannerGoods_plannerId와_date_둘다없으면_400() throws Exception {
        // given
        PlannerGoodsCreateRequest request = new PlannerGoodsCreateRequest(null, null, 58L);

        given(plannerGoodsService.addPlannerGoods(eq(USER_ID), any(PlannerGoodsCreateRequest.class)))
                .willThrow(new BadRequestException("plannerId 또는 date 중 하나는 반드시 필요합니다."));

        // when & then
        mockMvc.perform(post("/api/v1/planner-goods")
                        .with(authentication(loginAs(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("존재하지 않는 재고를 담으려 하면 404를 반환한다")
    void createPlannerGoods_존재하지않는_재고면_404() throws Exception {
        // given
        PlannerGoodsCreateRequest request = new PlannerGoodsCreateRequest(null, "2026-07-15", 999L);

        given(plannerGoodsService.addPlannerGoods(eq(USER_ID), any(PlannerGoodsCreateRequest.class)))
                .willThrow(new NotFoundException("존재하지 않는 재고입니다. id=999"));

        // when & then
        mockMvc.perform(post("/api/v1/planner-goods")
                        .with(authentication(loginAs(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
