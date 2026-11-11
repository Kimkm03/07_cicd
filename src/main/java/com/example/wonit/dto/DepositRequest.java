package com.example.wonit.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// 입금 요청 상자. memo 는 선택. @Valid 와 함께 쓰면 잘못된 값은 컨트롤러에 오기 전에 400 으로 막힌다
public record DepositRequest(
        @NotNull(message = "입금 금액을 입력하세요")
        @Positive(message = "입금 금액은 0보다 커야 합니다")
        @Digits(integer = 17, fraction = 2, message = "입금 금액은 소수 둘째 자리까지 입력하세요")   // DB 칸 DECIMAL(19,2) 과 같은 모양
        BigDecimal amount,
        @Size(max = 100, message = "메모는 100자까지 입력하세요")
        String memo) { }
