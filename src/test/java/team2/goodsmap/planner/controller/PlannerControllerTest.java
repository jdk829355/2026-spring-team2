package team2.goodsmap.planner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import team2.goodsmap.global.config.SecurityConfig;
import team2.goodsmap.global.exception.BadRequestException;
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.global.jwt.JwtTokenProvider;
import team2.goodsmap.planner.dto.request.PlannerCreateRequest;
import team2.goodsmap.planner.dto.response.PlannerDetailResponse;
import team2.goodsmap.planner.dto.response.PlannerGoodsResponse;
import team2.goodsmap.planner.dto.response.PlannerListResponse;
import team2.goodsmap.planner.dto.response.PlannerResponse;
import team2.goodsmap.planner.dto.response.PlannerSummary;
import team2.goodsmap.planner.service.PlannerService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PlannerController의 HTTP 계층만 검증한다. (서비스는 @MockitoBean으로 대체)
 *
 * 실제 서버에서는 JwtAuthenticationFilter가 JWT를 해독해 SecurityContext에 userId를 넣지만,
 * 여기서는 .with(authentication(...))으로 "이미 로그인된 상태"를 직접 만들어 준다.
 * 덕분에 진짜 토큰을 발급하지 않고도 @AuthenticationPrincipal Long userId 주입을 검증할 수 있다.
 * (JWT 해독 자체의 검증은 JwtTokenProvider 테스트의 몫)
 *
 * spring-security-test 의존성이 필요하다.
 * (testImplementation 'org.springframework.security:spring-security-test')
 */
@WebMvcTest(PlannerController.class)
@Import({SecurityConfig.class})
class PlannerControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long PLANNER_ID = 21L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlannerService plannerService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private UsernamePasswordAuthenticationToken loginAs(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    // ===== POST /api/v1/planners =====

    @Test
    @DisplayName("POST /api/v1/planners - 플래너를 생성한다")
    void 플래너_생성_성공() throws Exception {
        // given
        PlannerCreateRequest request = new PlannerCreateRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "title", "7월 굿즈 투어");
        org.springframework.test.util.ReflectionTestUtils.setField(request, "date", LocalDate.of(2026, 7, 15));

        given(plannerService.createPlanner(eq(USER_ID), any(PlannerCreateRequest.class)))
                .willReturn(PlannerResponse.builder()
                        .id(PLANNER_ID).userId(USER_ID)
                        .title("7월 굿즈 투어").date(LocalDate.of(2026, 7, 15))
                        .build());

        // when & then
        mockMvc.perform(post("/api/v1/planners")
                        .with(authentication(loginAs(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(21))
                .andExpect(jsonPath("$.data.title").value("7월 굿즈 투어"));
    }

    @Test
    @DisplayName("title이 없으면 400을 반환한다 (@Valid @NotBlank)")
    void 생성_title누락시_400() throws Exception {
        String body = """
                {"date": "2026-07-15"}
                """;

        mockMvc.perform(post("/api/v1/planners")
                        .with(authentication(loginAs(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // 검증에서 막혔으므로 서비스까지 내려가지 않는다
        verify(plannerService, never()).createPlanner(any(), any());
    }

    @Test
    @DisplayName("title이 공백뿐이면 400을 반환한다 (@NotBlank)")
    void 생성_title공백이면_400() throws Exception {
        String body = """
                {"title": "   ", "date": "2026-07-15"}
                """;

        mockMvc.perform(post("/api/v1/planners")
                        .with(authentication(loginAs(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(plannerService, never()).createPlanner(any(), any());
    }

    @Test
    @DisplayName("date가 없으면 400을 반환한다 (@Valid @NotNull)")
    void 생성_date누락시_400() throws Exception {
        String body = """
                {"title": "7월 굿즈 투어"}
                """;

        mockMvc.perform(post("/api/v1/planners")
                        .with(authentication(loginAs(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(plannerService, never()).createPlanner(any(), any());
    }

    // ===== GET /api/v1/planners =====

    @Test
    @DisplayName("GET /api/v1/planners?month - 목록과 통계를 반환한다")
    void 목록조회_성공() throws Exception {
        // given
        given(plannerService.getMyPlanners(USER_ID, "2026-07"))
                .willReturn(PlannerListResponse.builder()
                        .totalPlans(2).visitDays(1)
                        .planners(List.of(PlannerSummary.builder()
                                .id(PLANNER_ID).title("7월 굿즈 투어")
                                .date(LocalDate.of(2026, 7, 15)).goodsCount(3L)
                                .build()))
                        .build());

        // when & then
        mockMvc.perform(get("/api/v1/planners")
                        .param("month", "2026-07")
                        .with(authentication(loginAs(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPlans").value(2))
                .andExpect(jsonPath("$.data.visitDays").value(1))
                .andExpect(jsonPath("$.data.planners[0].goodsCount").value(3));
    }

    @Test
    @DisplayName("month 형식이 잘못되면 500이 아니라 400을 반환한다")
    void 목록조회_month형식오류시_400() throws Exception {
        // given
        given(plannerService.getMyPlanners(USER_ID, "2026-13"))
                .willThrow(new BadRequestException("month 형식이 올바르지 않습니다. (yyyy-MM) 입력값: 2026-13"));

        // when & then
        mockMvc.perform(get("/api/v1/planners")
                        .param("month", "2026-13")
                        .with(authentication(loginAs(USER_ID))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ===== GET /api/v1/planners/{id} =====

    @Test
    @DisplayName("GET /api/v1/planners/{id} - 담긴 굿즈와 스토어 정보까지 반환한다")
    void 상세조회_성공() throws Exception {
        // given
        PlannerGoodsResponse goods = PlannerGoodsResponse.builder()
                .id(103L).storeGoodsId(58L)
                .goodsName("아크릴 스탠드").animationTitle("주술회전")
                .price(15000).stock(30).imagePath("img.png")
                .store(PlannerGoodsResponse.StoreInfo.builder()
                        .id(7L).name("애니메이트 홍대점").address("서울 마포구 양화로 100")
                        .build())
                .build();

        given(plannerService.getPlannerDetail(USER_ID, PLANNER_ID))
                .willReturn(PlannerDetailResponse.builder()
                        .id(PLANNER_ID).userId(USER_ID)
                        .title("7월 굿즈 투어").date(LocalDate.of(2026, 7, 15))
                        .goods(List.of(goods))
                        .build());

        // when & then
        mockMvc.perform(get("/api/v1/planners/{plannerId}", PLANNER_ID)
                        .with(authentication(loginAs(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(21))
                .andExpect(jsonPath("$.data.goods[0].goodsName").value("아크릴 스탠드"))
                .andExpect(jsonPath("$.data.goods[0].animationTitle").value("주술회전"))
                .andExpect(jsonPath("$.data.goods[0].price").value(15000))
                .andExpect(jsonPath("$.data.goods[0].store.name").value("애니메이트 홍대점"));
    }

    @Test
    @DisplayName("남의 플래너를 조회하면 404를 반환한다")
    void 상세조회_남의플래너면_404() throws Exception {
        // given
        given(plannerService.getPlannerDetail(USER_ID, PLANNER_ID))
                .willThrow(new NotFoundException("본인의 플래너가 아닙니다. id=" + PLANNER_ID));

        // when & then
        mockMvc.perform(get("/api/v1/planners/{plannerId}", PLANNER_ID)
                        .with(authentication(loginAs(USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ===== PATCH /api/v1/planners/{id} =====

    @Test
    @DisplayName("PATCH - title만 보내도 정상 수정된다 (부분 수정)")
    void 수정_title만_성공() throws Exception {
        // given
        given(plannerService.updatePlanner(eq(USER_ID), eq(PLANNER_ID), any()))
                .willReturn(PlannerResponse.builder()
                        .id(PLANNER_ID).userId(USER_ID)
                        .title("수정된 제목").date(LocalDate.of(2026, 7, 15))
                        .build());

        String body = """
                {"title": "수정된 제목"}
                """;

        // when & then
        mockMvc.perform(patch("/api/v1/planners/{plannerId}", PLANNER_ID)
                        .with(authentication(loginAs(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 제목"));
    }

    @Test
    @DisplayName("PATCH - 아무 필드도 안 보내면 400을 반환한다 (@AssertTrue)")
    void 수정_빈요청이면_400() throws Exception {
        String body = "{}";

        mockMvc.perform(patch("/api/v1/planners/{plannerId}", PLANNER_ID)
                        .with(authentication(loginAs(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(plannerService, never()).updatePlanner(any(), any(), any());
    }

    @Test
    @DisplayName("PATCH - title을 공백으로 보내면 400을 반환한다 (@AssertTrue)")
    void 수정_title공백이면_400() throws Exception {
        String body = """
                {"title": "  "}
                """;

        mockMvc.perform(patch("/api/v1/planners/{plannerId}", PLANNER_ID)
                        .with(authentication(loginAs(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(plannerService, never()).updatePlanner(any(), any(), any());
    }

    // ===== DELETE =====

    @Test
    @DisplayName("DELETE /api/v1/planners/{id} - 플래너를 삭제한다")
    void 삭제_성공() throws Exception {
        // given
        willDoNothing().given(plannerService).deletePlanner(USER_ID, PLANNER_ID);

        // when & then
        mockMvc.perform(delete("/api/v1/planners/{plannerId}", PLANNER_ID)
                        .with(authentication(loginAs(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(plannerService).deletePlanner(USER_ID, PLANNER_ID);
    }

    @Test
    @DisplayName("DELETE /api/v1/planners/{id}/goods/{goodsId} - 담은 굿즈를 취소한다")
    void 굿즈취소_성공() throws Exception {
        // given
        willDoNothing().given(plannerService).removeGoods(USER_ID, PLANNER_ID, 103L);

        // when & then
        mockMvc.perform(delete("/api/v1/planners/{plannerId}/goods/{plannerGoodsId}", PLANNER_ID, 103L)
                        .with(authentication(loginAs(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(plannerService).removeGoods(USER_ID, PLANNER_ID, 103L);
    }

    @Test
    @DisplayName("해당 플래너에 없는 굿즈를 취소하려 하면 404를 반환한다")
    void 굿즈취소_없는굿즈면_404() throws Exception {
        // given
        willThrow(new NotFoundException("해당 플래너에 담긴 굿즈를 찾을 수 없습니다."))
                .given(plannerService).removeGoods(USER_ID, PLANNER_ID, 999L);

        // when & then
        mockMvc.perform(delete("/api/v1/planners/{plannerId}/goods/{plannerGoodsId}", PLANNER_ID, 999L)
                        .with(authentication(loginAs(USER_ID))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
