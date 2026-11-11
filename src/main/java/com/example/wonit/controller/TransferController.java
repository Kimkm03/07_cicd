package com.example.wonit.controller;

import com.example.wonit.dto.TransactionResponse;
import com.example.wonit.dto.TransferRequest;
import com.example.wonit.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

// POST /api/transfers — React 가 부르는 주소. (TransactionController 는 /transactions 아래라서 따로 만들었다)
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final AccountService accountService;

    // Idempotency-Key 헤더는 아직 읽지 않는다 (무시) — 같은 요청을 두 번 보내면 두 번 이체된다. 막는 원리는 Day 1 멱등성에서 다룬다 (이 수업에서는 코드로 만들지 않는다)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TransferRequest request) {
        return TransactionResponse.from(accountService.transfer(Integer.valueOf(jwt.getSubject()), request));
    }
}
