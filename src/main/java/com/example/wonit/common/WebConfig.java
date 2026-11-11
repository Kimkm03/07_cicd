package com.example.wonit.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 🌐 CORS: 브라우저는 다른 주소(React: localhost:5173)에서 온 요청을 서버가 허락해야만 통과시킨다
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)   // Page 를 { content, page: {size, number, totalElements, totalPages} } 로 안정된 JSON 모양으로 내보낸다
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "https://myname.woncare.shop")   // 🚨 "*" 로 모두에게 열지 않는다. 뒤의 주소는 내 도메인으로 바꾼다
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("*")
                .exposedHeaders("X-Request-Id", "Location");  // 응답 헤더를 JS 에서 읽으려면 노출 허락이 따로 필요하다
    }
}
