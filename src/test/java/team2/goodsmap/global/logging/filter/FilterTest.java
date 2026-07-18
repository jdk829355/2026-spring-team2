package team2.goodsmap.global.filter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;

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

    @Test
    void 요청_정보를_JSON_필드로_기록한다() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getHeader("X-Request-Id")).thenReturn("structured-log-test-id");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/stores");
        when(response.getStatus()).thenReturn(200);

        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            filter.doFilterInternal(request, response, chain);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        ILoggingEvent loggingEvent = appender.list.getFirst();
        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(logger.getLoggerContext());
        encoder.start();

        try {
            String encodedLog = new String(encoder.encode(loggingEvent), StandardCharsets.UTF_8);
            JsonNode json = new ObjectMapper().readTree(encodedLog);

            assertThat(json.get("message").asText()).isEqualTo("요청 완료");
            assertThat(json.get("event").asText()).isEqualTo("REQUEST_COMPLETED");
            assertThat(json.get("method").asText()).isEqualTo("GET");
            assertThat(json.get("uri").asText()).isEqualTo("/api/v1/stores");
            assertThat(json.get("status").asInt()).isEqualTo(200);
            assertThat(json.get("status").isNumber()).isTrue();
            assertThat(json.get("durationMs").isNumber()).isTrue();
        } finally {
            encoder.stop();
        }
    }
}
