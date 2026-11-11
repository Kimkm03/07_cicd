package com.example.wonit;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 입금 · 이체 흐름. @Transactional = 테스트가 끝나면 변경을 되돌려서 다른 테스트의 데이터(잔액 · 거래 건수)를 건드리지 않는다
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransferFlowTest {

    static final String SAVING = "1002-345-112233";    // yeonji 의 정상 계좌 (잔액 1,099,500)
    static final String WOORI = "1002-555-667788";     // woori 의 계좌 (잔액 500,000)
    static final String DORMANT = "1002-345-998877";   // yeonji 의 휴면 계좌

    @Autowired
    MockMvc mockMvc;

    String bearer;   // yeonji 의 토큰

    String bearerOf(String loginId) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"" + loginId + "\",\"password\":\"1234\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonMapper.builder().build().readTree(body).get("accessToken").asText();
    }

    @BeforeEach
    void login() throws Exception {
        bearer = bearerOf("yeonji");
    }

    MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    @DisplayName("입금 → 201, 잔액 1,099,500 + 10,000")
    void deposit() throws Exception {
        mockMvc.perform(json(post("/api/accounts/" + SAVING + "/deposits"), "{\"amount\":10000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.txType").value("입금"))
                .andExpect(jsonPath("$.balanceAfter").value(1109500));
    }

    @Test
    @DisplayName("이체 → 201, 보낸 계좌 -5,000 · 받은 계좌 +5,000")
    void transfer() throws Exception {
        mockMvc.perform(json(post("/api/transfers"),
                        "{\"fromAccountNo\":\"" + SAVING + "\",\"toAccountNo\":\"" + WOORI + "\",\"amount\":5000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.txType").value("출금"))
                .andExpect(jsonPath("$.counterparty").value(WOORI));

        mockMvc.perform(get("/api/accounts/" + SAVING).header("Authorization", bearer))
                .andExpect(jsonPath("$.balance").value(1094500));
        mockMvc.perform(get("/api/accounts/" + WOORI).header("Authorization", bearerOf("woori")))   // 남의 계좌는 woori 로 로그인해야 보인다
                .andExpect(jsonPath("$.balance").value(505000));
    }

    @Test
    @DisplayName("남의 계좌에서 이체 → 403")
    void transferFromOthers() throws Exception {
        mockMvc.perform(json(post("/api/transfers"),
                        "{\"fromAccountNo\":\"" + WOORI + "\",\"toAccountNo\":\"" + SAVING + "\",\"amount\":5000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("휴면 계좌에 입금 → 409 ACCOUNT_DORMANT")
    void depositToDormant() throws Exception {
        mockMvc.perform(json(post("/api/accounts/" + DORMANT + "/deposits"), "{\"amount\":10000}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DORMANT"));
    }

    @Test
    @DisplayName("같은 계좌로 이체 → 400")
    void transferToSameAccount() throws Exception {
        mockMvc.perform(json(post("/api/transfers"),
                        "{\"fromAccountNo\":\"" + SAVING + "\",\"toAccountNo\":\"" + SAVING + "\",\"amount\":5000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("남(woori)의 계좌에 입금 → 403")
    void depositToOthers() throws Exception {
        mockMvc.perform(json(post("/api/accounts/" + WOORI + "/deposits"), "{\"amount\":10000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("소수 셋째 자리 금액 → 400 과 DTO 의 메시지")
    void depositWithTooManyDecimals() throws Exception {
        mockMvc.perform(json(post("/api/accounts/" + SAVING + "/deposits"), "{\"amount\":1.234}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("입금 금액은 소수 둘째 자리까지 입력하세요"));
    }
}
