package com.example.wonit.ai;

import com.example.wonit.dto.AccountResponse;
import com.example.wonit.dto.CategorySpending;
import com.example.wonit.dto.MonthlySummary;
import com.example.wonit.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

// 🤖 AI 에게 "도구" 로 건네는 메서드 모음 (Tool Calling)
//   - 질문을 보고 어떤 도구를 부를지는 AI 가 고르지만, 실제 실행은 우리 서버가 한다
//   - 숫자는 DB 가 계산하고(ReportService), 문장은 AI 가 만든다
// 스프링 Bean 이 아니다: 요청마다 그 고객의 customerId 를 넣어 새로 만든다 (Day 2 JWT 부터는 토큰 주인)
@Slf4j
public class BankTools {

    private final Integer customerId;   // 🔐 AI 가 바꿀 수 없다 (도구 인자가 아니라 객체 필드)
    private final ReportService reportService;

    public BankTools(Integer customerId, ReportService reportService) {
        this.customerId = customerId;
        this.reportService = reportService;
    }

    @Tool(description = "로그인한 고객의 계좌 목록과 잔액, 상태(정상 · 지급정지 · 휴면)")
    public List<AccountResponse> myAccounts() {
        log.info("AI 가 도구 호출: myAccounts()");
        return reportService.accounts(customerId);
    }

    @Tool(description = "로그인한 고객의 '정상' 상태 계좌번호 목록 (/api/accounts/report1 과 같은 계산을 이 고객의 계좌로만)")
    public List<String> normalAccountNos() {
        log.info("AI 가 도구 호출: normalAccountNos()");
        return reportService.normalAccountNos(customerId);
    }

    @Tool(description = "로그인한 고객의 적금 계좌 잔액 합계 (/api/accounts/report2 와 같은 계산을 이 고객의 계좌로만)")
    public BigDecimal savingBalanceSum() {
        log.info("AI 가 도구 호출: savingBalanceSum()");
        return reportService.savingBalanceSum(customerId);
    }

    @Tool(description = "한 달 동안 카테고리별 출금(소비) 합계. month 는 yyyy-MM 형식")
    public List<CategorySpending> spendingByCategory(@ToolParam(description = "조회할 달, 예: 2026-09") String month) {
        log.info("AI 가 도구 호출: spendingByCategory({})", month);
        return reportService.spending(customerId, YearMonth.parse(month));   // 형식이 틀리면 예외 → Spring AI 가 오류를 AI 에게 돌려준다
    }

    @Tool(description = "최근 몇 달의 월별 입금 · 출금 합계")
    public List<MonthlySummary> monthlySummary(@ToolParam(description = "최근 몇 달 (1~12)") int months) {
        log.info("AI 가 도구 호출: monthlySummary({})", months);
        return reportService.monthly(customerId, months);
    }
}
