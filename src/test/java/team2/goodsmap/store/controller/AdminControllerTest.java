package team2.goodsmap.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import team2.goodsmap.global.config.SecurityConfig;
import team2.goodsmap.global.jwt.JwtTokenProvider;
import team2.goodsmap.store.dto.request.AddStoreAdminRequest;
import team2.goodsmap.store.dto.request.CreateStoreRequest;
import team2.goodsmap.store.dto.response.StoreAdminResponse;
import team2.goodsmap.store.dto.response.StoreResponse;
import team2.goodsmap.store.enums.StoreType;
import team2.goodsmap.store.service.StoreService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import({
        SecurityConfig.class
})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreService storeService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 사용자_관리_업체_조회() throws Exception {
        String token = "test-access-token";

        StoreResponse response =
                new StoreResponse(1L, "애니메이트", "다 있어요", StoreType.POPUP, LocalDate.now(), LocalDate.now().plusDays(1), "서울특별시 마포구 양화로 188", BigDecimal.valueOf(37.557743), BigDecimal.valueOf(126.926487));

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        given(storeService.getStoreByUserId(1L)).willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/stores/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("애니메이트"));

        verify(storeService).getStoreByUserId(1L);
    }

    @Test
    void 업체_생성() throws Exception {
        // given
        String token = "test-access-token";
        CreateStoreRequest request = createStoreRequest(LocalDate.now(), LocalDate.now().plusDays(1));
        StoreResponse response =
                new StoreResponse(1L, "애니메이트", "다 있어요", StoreType.POPUP, LocalDate.now(), LocalDate.now().plusDays(1), "서울특별시 마포구 양화로 188", BigDecimal.valueOf(37.557743), BigDecimal.valueOf(126.926487));

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        given(storeService.createStore(any(CreateStoreRequest.class), eq(1L)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("애니메이트"))
                .andExpect(jsonPath("$.data.description").value("다 있어요"));

        verify(storeService).createStore(any(CreateStoreRequest.class), eq(1L));
    }


    @Test
    void 업체_생성_업체명이_빈값이면_400() throws Exception {
        String token = "test-access-token";
        CreateStoreRequest request = new CreateStoreRequest(
                "  ",
                "다 있어요",
                StoreType.POPUP,
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                "서울특별시 마포구 양화로 188",
                BigDecimal.valueOf(37.557743),
                BigDecimal.valueOf(126.926487)
        );

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        mockMvc.perform(post("/api/v1/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void 업체_생성_주소가_빈값이면_400() throws Exception {
        String token = "test-access-token";
        CreateStoreRequest request = new CreateStoreRequest(
                "애니메이트",
                "다 있어요",
                StoreType.POPUP,
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                "",
                BigDecimal.valueOf(37.557743),
                BigDecimal.valueOf(126.926487)
        );

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        mockMvc.perform(post("/api/v1/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void 관리자_추가() throws Exception {
        String token = "test-access-token";
        AddStoreAdminRequest request = new AddStoreAdminRequest("test2@example.com");

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");
        given(storeService.createStoreAdmin(any(AddStoreAdminRequest.class), anyLong(), anyLong())).willReturn(new StoreAdminResponse(1L, 1L, 1L, "test-user-name", "test2@example.com"));

        mockMvc.perform(post("/api/v1/stores/1/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(storeService).createStoreAdmin(any(AddStoreAdminRequest.class), eq(1L), eq(1L));
    }

    @Test
    void 관리자_조회() throws Exception {
        String token = "test-access-token";

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        given(storeService.getStoreAdmin(1L, 1L)).willReturn(List.of(new StoreAdminResponse(1L, 1L, 1L, "test-user-name", "test2@example.com")));

        mockMvc.perform(get("/api/v1/stores/1/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 관리자_추가_이메일_누락() throws Exception {
        String token = "test-access-token";
        AddStoreAdminRequest request = new AddStoreAdminRequest("");

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        given(storeService.createStoreAdmin(any(AddStoreAdminRequest.class), anyLong(), anyLong())).willReturn(new StoreAdminResponse(1L, 1L, 1L, "test-user-name", "test2@example.com"));

        mockMvc.perform(post("/api/v1/stores/1/admin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void 관리자_삭제_성공() throws Exception {
        String token = "test-access-token";

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        willDoNothing().given(storeService).deleteStoreAdmin(1L, 1L, 2L);

        mockMvc.perform(delete("/api/v1/stores/1/admin/2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(storeService).deleteStoreAdmin(1L, 1L, 2L);
    }

    @Test
    void 관리자_삭제_권한_없음_400() throws Exception {
        String token = "test-access-token";

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        willThrow(new IllegalArgumentException("해당 업체의 관리 권한이 없습니다."))
                .given(storeService).deleteStoreAdmin(1L, 1L, 2L);

        mockMvc.perform(delete("/api/v1/stores/1/admin/2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("해당 업체의 관리 권한이 없습니다."));
    }

    private CreateStoreRequest createStoreRequest(LocalDate startDate, LocalDate endDate) {
        return new CreateStoreRequest(
                "애니메이트",
                "다 있어요",
                StoreType.POPUP,
                startDate,
                endDate,
                "서울특별시 마포구 양화로 188",
                BigDecimal.valueOf(37.557743),
                BigDecimal.valueOf(126.926487)
        );
    }
}
