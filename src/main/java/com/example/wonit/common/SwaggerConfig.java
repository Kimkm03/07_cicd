package com.example.wonit.common;

// common/SwaggerConfig.java
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger UI(http://localhost:8080/swagger-ui/index.html) 첫 화면의 제목과 설명.
// API 목록 자체는 springdoc 이 컨트롤러를 읽어서 자동으로 만듭니다
// 이 파일이 없어도 Swagger 는 뜹니다(제목만 기본값).
// 🏦 운영 서버에서는 Swagger 를 끕니다(springdoc.api-docs.enabled=false).
// 주소 목록이 곧 공격 지도가 되기 때문입니.
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("우리WON 뱅킹 API")
                        .description("""
                        Try it out → Execute 로 바로 요청을 보냅니다. 브라우저 주소창은 GET 만 되지만 여기서는 POST 도 됩니다.
                        accountNo: 1002-345-678901로 테스트하세요.""")
                        .version("1.0"))
                // 🔐 Swagger 오른쪽 위에 Authorize 버튼이 생긴다. 로그인 API 로 받은 accessToken 을 넣으면 모든 요청에 붙는다
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
