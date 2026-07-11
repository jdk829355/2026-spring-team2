package team2.goodsmap.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
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
import team2.goodsmap.global.exception.NotFoundException;
import team2.goodsmap.global.jwt.JwtTokenProvider;
import team2.goodsmap.global.s3.S3Service;
import team2.goodsmap.goods.dto.CreateGoodsRequest;
import team2.goodsmap.goods.dto.GoodsResponse;
import team2.goodsmap.goods.service.GoodsService;
import team2.goodsmap.store.dto.request.AddExistingStoreGoodsRequest;
import team2.goodsmap.store.dto.request.AddImagePathRequest;
import team2.goodsmap.store.dto.request.AddNewStoreGoodsRequest;
import team2.goodsmap.store.dto.request.AddStoreAdminRequest;
import team2.goodsmap.store.dto.request.CreateStoreRequest;
import team2.goodsmap.store.dto.request.UpdateStoreGoodsRequest;
import team2.goodsmap.store.dto.request.UpdateStoreRequest;
import team2.goodsmap.store.dto.response.StoreAdminResponse;
import team2.goodsmap.store.dto.response.StoreDetailResponse;
import team2.goodsmap.store.dto.response.StoreGoodsResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private GoodsService goodsService;

    @MockitoBean
    private S3Service s3Service;

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
                .andExpect(jsonPath("$.status").value(400))
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
                .andExpect(jsonPath("$.status").value(400))
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
                .andExpect(jsonPath("$.status").value(400))
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

    @Test
    void 새상품으로_storeGoods를_추가한다() throws Exception {
        String token = "test-access-token";
        AddNewStoreGoodsRequest request = new AddNewStoreGoodsRequest(
                new CreateGoodsRequest(1L, "아크릴 스탠드"),
                15000,
                30
        );
        GoodsResponse goodsResponse = new GoodsResponse(10L, "아크릴 스탠드", 1L, "하이큐");
        StoreGoodsResponse storeGoodsResponse = new StoreGoodsResponse(100L, 1L, goodsResponse, 15000, 30, "https://example.com/new.png");

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");
        given(goodsService.createGoods(any(CreateGoodsRequest.class))).willReturn(goodsResponse);
        given(storeService.createStoreGoods(any(AddNewStoreGoodsRequest.class), any(GoodsResponse.class), eq(1L), eq(1L)))
                .willReturn(storeGoodsResponse);

        mockMvc.perform(post("/api/v1/stores/1/goods/new")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.goodsInfo.id").value(10))
                .andExpect(jsonPath("$.data.price").value(15000));

        verify(goodsService).createGoods(any(CreateGoodsRequest.class));
        verify(storeService).createStoreGoods(any(AddNewStoreGoodsRequest.class), any(GoodsResponse.class), eq(1L), eq(1L));
    }

    @Test
    void 기존상품으로_storeGoods를_추가한다() throws Exception {
        String token = "test-access-token";
        AddExistingStoreGoodsRequest request = new AddExistingStoreGoodsRequest(
                10L,
                5000,
                12
        );
        GoodsResponse goodsResponse = new GoodsResponse(10L, "포토카드", 1L, "슬램덩크");
        StoreGoodsResponse storeGoodsResponse = new StoreGoodsResponse(101L, 1L, goodsResponse, 5000, 12, "https://example.com/existing.png");

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");
        given(storeService.createStoreGoods(any(AddExistingStoreGoodsRequest.class), eq(1L), eq(1L)))
                .willReturn(storeGoodsResponse);

        mockMvc.perform(post("/api/v1/stores/1/goods")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.goodsInfo.id").value(10))
                .andExpect(jsonPath("$.data.stock").value(12));

        verify(storeService).createStoreGoods(any(AddExistingStoreGoodsRequest.class), eq(1L), eq(1L));
    }

    @Test
    void 상품_삭제_성공() throws Exception {
        String token = "test-access-token";

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        willDoNothing().given(storeService).deleteStoreGoods(1L, 100L, 1L);

        mockMvc.perform(delete("/api/v1/stores/1/goods/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(storeService).deleteStoreGoods(1L, 100L, 1L);
    }

    @Test
    void 상품_삭제_권한_없음_400() throws Exception {
        String token = "test-access-token";

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        willThrow(new IllegalArgumentException("삭제 권한이 없습니다."))
                .given(storeService).deleteStoreGoods(1L, 100L, 1L);

        mockMvc.perform(delete("/api/v1/stores/1/goods/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("삭제 권한이 없습니다."));
    }

    @Test
    @DisplayName("PATCH /api/v1/stores/{storeId}/goods/{storeGoodsId} - 스토어 상품 정보를 수정한다")
    void 스토어_상품_정보_수정() throws Exception {
        // given
        String token = "test-access-token";
        UpdateStoreGoodsRequest request = new UpdateStoreGoodsRequest(20000, 50, "https://example.com/updated.png");
        GoodsResponse goodsResponse = new GoodsResponse(10L, "아크릴 스탠드", 1L, "하이큐");
        StoreGoodsResponse storeGoodsResponse = new StoreGoodsResponse(100L, 1L, goodsResponse, 20000, 50, "https://example.com/updated.png");

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        given(storeService.modifyStoreGoods(eq(1L), eq(100L), any(UpdateStoreGoodsRequest.class), eq(1L)))
                .willReturn(storeGoodsResponse);

        // when & then
        mockMvc.perform(patch("/api/v1/stores/1/goods/100")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.price").value(20000))
                .andExpect(jsonPath("$.data.stock").value(50))
                .andExpect(jsonPath("$.data.imagePath").value("https://example.com/updated.png"));

        verify(storeService).modifyStoreGoods(eq(1L), eq(100L), any(UpdateStoreGoodsRequest.class), eq(1L));
    }

    @Test
    void 스토어_수정() throws Exception {
        String token = "test-access-token";
        UpdateStoreRequest request = new UpdateStoreRequest(
                "수정된 이름", "수정된 설명", StoreType.POPUP,
                LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30),
                "변경된 주소", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0)
        );
        StoreResponse response =
                new StoreResponse(1L, "수정된 이름", "수정된 설명", StoreType.POPUP,
                        LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30),
                        "변경된 주소", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0));

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        given(storeService.updateStore(any(UpdateStoreRequest.class), eq(1L), eq(1L)))
                .willReturn(response);

        mockMvc.perform(patch("/api/v1/stores/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("수정된 이름"))
                .andExpect(jsonPath("$.data.description").value("수정된 설명"))
                .andExpect(jsonPath("$.data.address").value("변경된 주소"));

        verify(storeService).updateStore(any(UpdateStoreRequest.class), eq(1L), eq(1L));
    }

    @Test
    void 스토어_수정_권한_없음_400() throws Exception {
        String token = "test-access-token";
        UpdateStoreRequest request = new UpdateStoreRequest(
                "수정된 이름", null, null, null, null, null, null, null
        );

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        willThrow(new IllegalArgumentException("수정 권한이 없습니다."))
                .given(storeService).updateStore(any(UpdateStoreRequest.class), eq(1L), eq(1L));

        mockMvc.perform(patch("/api/v1/stores/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("수정 권한이 없습니다."));
    }

    @Test
    void 스토어_상세_조회() throws Exception {
        String token = "test-access-token";
        StoreDetailResponse response = new StoreDetailResponse(
                1L, "애니메이트", "다 있어요", StoreType.POPUP,
                LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31),
                "서울특별시 마포구 양화로 188",
                BigDecimal.valueOf(37.557743), BigDecimal.valueOf(126.926487),
                5L
        );

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        given(storeService.getStoreDetail(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/stores/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("애니메이트"))
                .andExpect(jsonPath("$.data.description").value("다 있어요"))
                .andExpect(jsonPath("$.data.type").value("POPUP"))
                .andExpect(jsonPath("$.data.address").value("서울특별시 마포구 양화로 188"))
                .andExpect(jsonPath("$.data.goodsCount").value(5));

        verify(storeService).getStoreDetail(1L);
    }

    @Test
    void 스토어_상세_조회_존재하지_않는_스토어_404() throws Exception {
        String token = "test-access-token";

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        given(storeService.getStoreDetail(999L))
                .willThrow(new NotFoundException("존재하지 않는 스토어입니다. id=999"));

        mockMvc.perform(get("/api/v1/stores/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("존재하지 않는 스토어입니다. id=999"));
    }

    @Test
    void storeGoods_이미지_경로_수정_성공() throws Exception {
        String token = "test-access-token";
        AddImagePathRequest request = new AddImagePathRequest(
                "stores/1/goods/100/images/550e8400-e29b-41d4-a716-446655440000.png");

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        willDoNothing().given(storeService).updateImagePath(eq(1L), eq(1L), eq(100L), any(AddImagePathRequest.class));

        mockMvc.perform(put("/api/v1/stores/1/goods/100/image-path")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(storeService).updateImagePath(eq(1L), eq(1L), eq(100L), any(AddImagePathRequest.class));
    }

    @Test
    void storeGoods_이미지_경로_수정_objectKey_빈값_400() throws Exception {
        String token = "test-access-token";
        AddImagePathRequest request = new AddImagePathRequest("");

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        mockMvc.perform(put("/api/v1/stores/1/goods/100/image-path")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void storeGoods_이미지_경로_수정_권한_없음_400() throws Exception {
        String token = "test-access-token";
        AddImagePathRequest request = new AddImagePathRequest(
                "stores/1/goods/100/images/550e8400-e29b-41d4-a716-446655440000.png");

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        willThrow(new IllegalArgumentException("이미지 경로 수정 권한이 없습니다."))
                .given(storeService).updateImagePath(eq(1L), eq(1L), eq(100L), any(AddImagePathRequest.class));

        mockMvc.perform(put("/api/v1/stores/1/goods/100/image-path")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미지 경로 수정 권한이 없습니다."));
    }

    @Test
    void storeGoods_이미지_경로_수정_상품_없음_400() throws Exception {
        String token = "test-access-token";
        AddImagePathRequest request = new AddImagePathRequest(
                "stores/1/goods/100/images/550e8400-e29b-41d4-a716-446655440000.png");

        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.isAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.getUserId(token)).willReturn(1L);
        given(jwtTokenProvider.getRole(token)).willReturn("STORE");

        willThrow(new IllegalArgumentException("해당 상품이 없습니다."))
                .given(storeService).updateImagePath(eq(1L), eq(1L), eq(100L), any(AddImagePathRequest.class));

        mockMvc.perform(put("/api/v1/stores/1/goods/100/image-path")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("해당 상품이 없습니다."));
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
