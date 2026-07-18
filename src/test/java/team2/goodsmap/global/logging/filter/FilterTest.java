package team2.goodsmap.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear(); // 테스트 간 MDC 오염 방지
    }

    @Test
    void 헤더에_requestId가_있으면_그대로_사용한다() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Request-Id")).thenReturn("fixed-test-id");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/goods");
        when(response.getStatus()).thenReturn(200);

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader("X-Request-Id", "fixed-test-id");
        verify(chain).doFilter(request, response); // 다음 필터/컨트롤러로 정상 전달됐는지
    }

    @Test
    void 헤더가_없으면_UUID를_새로_생성한다() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/planner-goods");
        when(response.getStatus()).thenReturn(201);

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(eq("X-Request-Id"), argThat(id -> id != null && !id.isBlank()));
    }

    @Test
    void 컨트롤러에서_예외가_터져도_MDC는_반드시_정리된다() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Request-Id")).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/goods");
        when(response.getStatus()).thenReturn(500);

        doThrow(new RuntimeException("boom")).when(chain).doFilter(request, response);

        try {
            filter.doFilterInternal(request, response, chain);
        } catch (RuntimeException ignored) {
            // 예외는 다시 던져지는 게 정상 (필터가 삼키면 안 됨)
        }

        assertThat(MDC.get("requestId")).isNull(); // finally에서 지워졌는지 확인
    }
}
