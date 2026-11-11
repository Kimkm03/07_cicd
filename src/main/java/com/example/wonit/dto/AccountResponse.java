package com.example.wonit.dto;

// 데이터를 final로 (중간에 변경하지 않는 대신 편하게 실어나르는 자바의 새 문법)
// dto.AccountResponse.java

import com.example.wonit.model.Account;

import java.math.BigDecimal;

// Controller <-> Service 사이에서만 동작
// 바깥(JSON)으로 내보내는 "안 바뀌는 상자". 바뀌는 상자(Account)는 class, 응답 상자는 record 다
// accountId 는 내부용 번호라서 밖으로 내보내지 않습니다
// public record AccountResponse(String accountNo, String accountType, long balance, String status) {
public record AccountResponse(String accountNo, String accountType, BigDecimal balance, String status) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getAccountNo(),
                account.getAccountType(),
                account.getBalance(),
                account.getStatus());
    }
}
