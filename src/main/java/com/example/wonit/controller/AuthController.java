package com.example.wonit.controller;

import com.example.wonit.dto.ErrorResponse;
import com.example.wonit.model.Customer;
import com.example.wonit.repository.CustomerRepository;
import com.example.wonit.service.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;

// 🔐 회원가입 · 로그인(토큰 발급) · "나는 누구인가". 회원은 customer 표(DB)에 있다
@RestController
@RequestMapping("/api")
public class AuthController {

    record LoginRequest(String loginId, String password) {}
    record LoginResponse(String accessToken, long expiresIn, String name) {}
    record MeResponse(String name, String grade) {}
    record SignupRequest(@NotBlank @Size(max = 30) String loginId,
                         @NotBlank @Size(min = 4, max = 72) String password,     // BCrypt 는 72바이트까지만 쓴다
                         @NotBlank @Size(max = 50) String name) {}
    record SignupResponse(Integer customerId, String loginId, String name, String accountNo) {}

    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final JwtEncoder jwtEncoder;
    private final PasswordEncoder passwordEncoder;
    private final long expirationMinutes;

    public AuthController(CustomerRepository customerRepository, CustomerService customerService,
                          JwtEncoder jwtEncoder, PasswordEncoder passwordEncoder,
                          @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        this.customerRepository = customerRepository;
        this.customerService = customerService;
        this.jwtEncoder = jwtEncoder;
        this.passwordEncoder = passwordEncoder;
        this.expirationMinutes = expirationMinutes;
    }

    // 회원가입 → 201. 아이디가 이미 있으면 409, 비밀번호가 4자 미만이면 400
    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        CustomerService.Joined joined = customerService.signup(request.loginId(), request.password(), request.name());
        return new SignupResponse(joined.customer().getCustomerId(), joined.customer().getLoginId(),
                joined.customer().getName(), joined.account().getAccountNo());
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Customer user = request.loginId() == null ? null
                : customerRepository.findByLoginId(request.loginId()).orElse(null);   // 아이디를 안 보내면 실패
        // 🚨 아이디가 없는 경우와 비밀번호가 틀린 경우의 메시지를 똑같이 한다 (어느 쪽이 틀렸는지 알려주지 않는다)
        if (user == null || request.password() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            return ResponseEntity.status(401)
                    .body(new ErrorResponse("LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다."));
        }
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.getCustomerId()))      // 토큰 주인 = 고객번호
                .claim("name", user.getName())
                .claim("grade", user.getGrade())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(expirationMinutes)))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return ResponseEntity.ok(new LoginResponse(token, expirationMinutes * 60, user.getName()));
    }

    // 토큰 안의 정보(claims)를 꺼내 쓴다. DB 조회가 필요 없다
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return new MeResponse(jwt.getClaimAsString("name"), jwt.getClaimAsString("grade"));
    }
}
