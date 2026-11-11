package com.example.wonit.service;

import com.example.wonit.dto.AccountResponse;
import com.example.wonit.dto.CategorySpending;
import com.example.wonit.dto.MonthlySummary;
import com.example.wonit.model.Account;
import com.example.wonit.model.Transaction;
import com.example.wonit.repository.AccountRepository;
import com.example.wonit.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

// 📊 숫자는 DB 가 계산한다. 이 서비스를 REST API(ReportController)와 AI 도구(BankTools)가 함께 쓴다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    // 한 달 동안 카테고리별 출금 합계 (큰 금액 먼저). [그달 1일 00:00, 다음달 1일 00:00)
    public List<CategorySpending> spending(Integer customerId, YearMonth month) {
        return transactionRepository.spendingByCategory(
                customerId, month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
    }

    // 최근 N달의 월별 입금 · 출금 합계 (오래된 달 → 최근 달)
    public List<MonthlySummary> monthly(Integer customerId, int months) {
        int count = Math.max(1, Math.min(months, 12));                  // 1~12 로 맞춘다
        YearMonth first = YearMonth.now().minusMonths(count - 1);
        List<Transaction> txs = transactionRepository
                .findByAccount_CustomerIdAndTxDatetimeGreaterThanEqual(customerId, first.atDay(1).atStartOfDay());

        List<MonthlySummary> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {                               // 거래가 없는 달도 0원으로 채운다
            YearMonth month = first.plusMonths(i);
            result.add(new MonthlySummary(month.toString(), sum(txs, month, "입금"), sum(txs, month, "출금")));
        }
        return result;
    }

    // 그 달의 입금(또는 출금) 금액 합계 (report2 의 reduce 와 같은 모양)
    private BigDecimal sum(List<Transaction> txs, YearMonth month, String txType) {
        return txs.stream()
                .filter(tx -> txType.equals(tx.getTxType()) && YearMonth.from(tx.getTxDatetime()).equals(month))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // 🔐 이 고객의 계좌 목록
    public List<AccountResponse> accounts(Integer customerId) {
        return accountRepository.findByCustomerId(customerId).stream().map(AccountResponse::from).toList();
    }

    // AccountController 의 /report1 · /report2 와 같은 계산. 다만 그쪽은 "모든 고객" 의 계좌를 보므로
    // 🔐 AI 도구에는 이 고객의 계좌(findByCustomerId)만 넣은 버전을 준다
    public List<String> normalAccountNos(Integer customerId) {
        return accountRepository.findByCustomerId(customerId).stream()
                .filter(account -> account.getStatus().equals("정상"))
                .map(Account::getAccountNo).toList();
    }

    public BigDecimal savingBalanceSum(Integer customerId) {
        return accountRepository.findByCustomerId(customerId).stream()
                .filter(account -> account.getAccountType().equals("적금"))
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
