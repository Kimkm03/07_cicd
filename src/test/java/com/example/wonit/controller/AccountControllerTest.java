package com.example.wonit.controller;

import com.example.wonit.exception.AccountNotFoundException;
import com.example.wonit.model.Account;
import com.example.wonit.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;   // 🔐 로그인한 척 (가짜 토큰)
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    @DisplayName("없는 계좌 조회 -> 404 ACCOUNT_NOT_FOUND")
    void notFound() throws Exception {
        given(accountService.getAccount("9999-000-000000"))
                .willThrow(new AccountNotFoundException("9999-000-000000 계좌를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/accounts/9999-000-000000").with(jwt().jwt(j -> j.subject("1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("계좌 조회 성공 → 200 과 계좌 정보")
    void findOne_ok() throws Exception {
        Account account = Account.open(1, "1111-111-111111", "입출금");
        account.deposit(new BigDecimal("50000.00"));
        given(accountService.getAccount(account.getAccountNo())).willReturn(account);

        mockMvc.perform(get("/api/accounts/" + account.getAccountNo()).with(jwt().jwt(j -> j.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNo").value(account.getAccountNo()))
                .andExpect(jsonPath("$.balance").value(50000.00))
                .andExpect(jsonPath("$.status").value("정상"));
    }
}