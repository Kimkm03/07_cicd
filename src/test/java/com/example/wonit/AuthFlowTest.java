package com.example.wonit;

import com.example.wonit.repository.CustomerRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 진짜 앱 전체를 띄워 "로그인 → 토큰 → 내 계좌만" 흐름을 확인한다
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CustomerRepository customerRepository;

    org.springframework.test.web.servlet.ResultActions signup(String id, String pw, String name) throws Exception {
        return mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginId\":\"" + id + "\",\"password\":\"" + pw + "\",\"name\":\"" + name + "\"}"));
    }

    String login(String id, String pw) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"" + id + "\",\"password\":\"" + pw + "\"}"))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("yeonji 로그인 → 토큰으로 계좌 조회 → 본인 계좌 3개만")
    void loginThenMyAccounts() throws Exception {
        String token = JsonMapper.builder().build().readTree(login("yeonji", "1234")).get("accessToken").asText();

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("woori 로그인 → 남(yeonji)의 계좌 조회 · 거래내역 · 상태 변경 → 403")
    void othersAccountForbidden() throws Exception {
        String token = JsonMapper.builder().build().readTree(login("woori", "1234")).get("accessToken").asText();

        mockMvc.perform(get("/api/accounts/1002-345-112233").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/accounts/1002-345-112233/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/accounts/1002-345-678901/status").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"정상\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("아이디 없이 로그인 → 401 (500 이 아니다)")
    void loginWithoutId() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 없이 계좌 조회 → 401")
    void noToken() throws Exception {
        mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("비밀번호가 틀리면 → 401 LOGIN_FAILED")
    void wrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"yeonji\",\"password\":\"0000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @Test
    @DisplayName("회원가입 → 201 · 입출금 계좌 1개 → 그 아이디로 로그인하면 내 계좌 1개 (0원)")
    void signupThenLogin() throws Exception {
        signup("newbie1", "abcd", "새회원")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loginId").value("newbie1"))
                .andExpect(jsonPath("$.accountNo").isNotEmpty());

        String token = JsonMapper.builder().build().readTree(login("newbie1", "abcd")).get("accessToken").asText();
        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accountType").value("입출금"))
                .andExpect(jsonPath("$[0].balance").value(0));
    }

    @Test
    @DisplayName("이미 있는 아이디로 가입 → 409, 비밀번호 4자 미만 → 400")
    void signupRejected() throws Exception {
        signup("yeonji", "abcd", "중복").andExpect(status().isConflict());
        signup("newbie2", "123", "짧은비번").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호는 평문이 아니라 BCrypt 해시로 저장된다")
    void passwordIsHashed() throws Exception {
        signup("newbie3", "abcd", "해시확인").andExpect(status().isCreated());

        String saved = customerRepository.findByLoginId("newbie3").orElseThrow().getPasswordHash();
        assertThat(saved).isNotEqualTo("abcd").startsWith("$2");
    }
}
