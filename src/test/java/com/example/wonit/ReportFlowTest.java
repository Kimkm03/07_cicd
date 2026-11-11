package com.example.wonit;

import com.example.wonit.ai.BankTools;
import com.example.wonit.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;   // 🔐 로그인한 척 (가짜 토큰)
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 리포트 API 와 AI 도구가 "내 거래만" 보는지 확인한다. 진짜 AI 는 부르지 않는다 (네트워크 · 키 필요)
@SpringBootTest
@AutoConfigureMockMvc
class ReportFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ReportService reportService;

    @Test
    @DisplayName("고객 1 의 2026-09 소비 → 이체 100500 이 먼저, 식비 12000 (1건)")
    void spendingOfCustomer1() throws Exception {
        mockMvc.perform(get("/api/reports/spending?month=2026-09").with(jwt().jwt(j -> j.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("이체"))      // 금액이 큰 순서
                .andExpect(jsonPath("$[0].total").value(100500))
                .andExpect(jsonPath("$[1].category").value("식비"))
                .andExpect(jsonPath("$[1].total").value(12000))
                .andExpect(jsonPath("$[1].count").value(1));
    }

    @Test
    @DisplayName("고객 2 는 같은 달에 소비가 없다 → 빈 목록 (남의 거래는 안 보인다)")
    void spendingOfCustomer2() throws Exception {
        mockMvc.perform(get("/api/reports/spending?month=2026-09").with(jwt().jwt(j -> j.subject("2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("토큰 없이 리포트 조회 → 401")
    void noToken() throws Exception {
        mockMvc.perform(get("/api/reports/spending")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("월별 합계는 요청한 달 수만큼, 오래된 달부터 나온다")
    void monthly() throws Exception {
        mockMvc.perform(get("/api/reports/monthly?months=3").with(jwt().jwt(j -> j.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("AI 도구 myAccounts() → 토큰 주인(고객 2)의 계좌만")
    void toolSeesOnlyMyAccounts() {
        var accounts = new BankTools(2, reportService).myAccounts();

        assertThat(accounts).extracting("accountNo").containsExactly("1002-555-667788");
    }

    @Test
    @DisplayName("AI 도구 report1 · report2 버전 → 고객 1 의 계좌로만 계산")
    void reportToolsUseOnlyMyAccounts() {
        BankTools tools = new BankTools(1, reportService);

        assertThat(tools.normalAccountNos()).containsExactly("1002-345-112233");
        assertThat(tools.savingBalanceSum()).isEqualByComparingTo("1596500");   // 1,099,500 + 497,000
        assertThat(new BankTools(2, reportService).savingBalanceSum()).isEqualByComparingTo("0");   // 고객 2 는 적금이 없다
    }

    @Test
    @DisplayName("새 대화(DELETE /api/ai/chat) → 204, 토큰 없이는 401")
    void clearChat() throws Exception {
        mockMvc.perform(delete("/api/ai/chat").with(jwt().jwt(j -> j.subject("1"))))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/ai/chat")).andExpect(status().isUnauthorized());
    }
}
