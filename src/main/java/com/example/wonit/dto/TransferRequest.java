package com.example.wonit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// 이체 요청 상자. 보내는 사람은 body 가 아니라 토큰으로 확인한다 (body 의 fromAccountNo 가 본인 것인지 서버가 검사)
public record TransferRequest(
        @NotBlank(message = "보내는 계좌번호를 입력하세요") String fromAccountNo,
        @NotBlank(message = "받는 계좌번호를 입력하세요") String toAccountNo,
        @NotNull(message = "이체 금액을 입력하세요")
        @Positive(message = "이체 금액은 0보다 커야 합니다")
        @Digits(integer = 17, fraction = 2, message = "이체 금액은 소수 둘째 자리까지 입력하세요")   // DB 칸 DECIMAL(19,2) 과 같은 모양
        BigDecimal amount,
        @Size(max = 100, message = "메모는 100자까지 입력하세요")
        String memo) { }
