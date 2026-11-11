package com.example.wonit.common;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 요청 번호(traceId)를 응답 헤더 X-Request-Id 로 돌려주는 필터. 번호는 추적 라이브러리가 만들어 로그(MDC)에도 넣어 준다
// 앞단이 traceparent 헤더를 보내면 그 번호를 이어 쓴다 (서비스가 여러 개여도 번호가 끊기지 않는다)
// 🚨 추적을 시작하는 필터(HIGHEST_PRECEDENCE + 1) "뒤" 에 돌아야 span 이 있다
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class RequestIdFilter extends OncePerRequestFilter {

    // ObjectProvider: 추적 라이브러리가 없는 테스트(@WebMvcTest 등)에서도 앱이 뜨게 "있으면 쓴다"
    private final ObjectProvider<Tracer> tracerProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Tracer tracer = tracerProvider.getIfAvailable();
        Span span = tracer != null ? tracer.currentSpan() : null;
        if (span != null) {
            response.setHeader("X-Request-Id", span.context().traceId());   // 손님이 문의할 때 말하는 번호
        }
        chain.doFilter(request, response);
    }
}
