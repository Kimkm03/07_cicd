package com.example.wonit.common;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

// 🔐 "누가 어디까지 들어올 수 있나" 를 정하는 곳. 토큰 검사는 스프링 시큐리티(리소스 서버)가 해 주므로 우리 코드는 설정뿐이다
@Configuration
public class SecurityConfig {

    // 토큰 서명에 쓰는 비밀키 (HS256 = 같은 키로 서명하고 같은 키로 검증)
    private final SecretKey key;

    public SecurityConfig(@Value("${jwt.secret}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder() {       // 토큰 만들기 (로그인 때 사용)
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder() {       // 토큰 검증하기 (모든 요청에서 자동 사용)
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {     // 🏦 비밀번호는 평문이 아니라 BCrypt 해시로만 보관한다
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())            // 🚨 이 줄이 없으면 WebConfig 의 CORS 설정이 무시되고 사전 확인(OPTIONS)이 401 이 된다
                .csrf(AbstractHttpConfigurer::disable)      // 쿠키 세션을 안 쓰고 토큰을 쓰므로 CSRF 는 끈다
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))   // 서버는 로그인 상태를 저장하지 않는다
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))     // Authorization: Bearer 토큰 검사
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))               // H2 콘솔이 iframe 을 쓴다
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/login", "/api/auth/signup", "/h2-console/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/hello/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()   // 서버가 살았는지는 누구나 (로드밸런서가 확인한다). /actuator/metrics 는 토큰이 필요
                        .requestMatchers("/actuator/prometheus").permitAll()   // Prometheus 는 토큰을 못 보낸다. 밖에서는 ALB 가 /actuator/* 를 403 으로 막는다
                        .anyRequest().authenticated());     // 나머지는 토큰이 있어야 한다 (없으면 401)
        return http.build();
    }
}
