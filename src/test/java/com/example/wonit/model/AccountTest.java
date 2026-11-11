package com.example.wonit.model;

import com.example.wonit.exception.AccountFrozenException;
import com.example.wonit.exception.InsufficientBalanceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class AccountTest {

    @Test
    @DisplayName("정상 계좌에서 출금하면 잔액이 줄어든다")
    void withdraw_reducesBalance() {
        // given  (준비: 이런 계좌가 있을 때)
        Account account = Account.open(1, "1002-111-111111","입출금");
        account.deposit(new BigDecimal("100000"));
        // when   (실행)
        account.withdraw(new BigDecimal("30000"));
        // then   (확인)
        assertThat(account.getBalance()).isEqualByComparingTo("70000");
    }

    @Test
    @DisplayName("잔액보다 큰 금액은 출금할 수 없다")
    void withdraw_insufficientBalance() {
        Account account = Account.open(1, "1002-111-111111","입출금");
        account.deposit(new BigDecimal("5000"));

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("50000")))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(account.getBalance()).isEqualByComparingTo("5000");   // 🚨 막힌 출금이 잔액을 건드리지 않았는지도 확인한다
    }

    // 실습1: 경계값
    @Test
    void withdraw_exactBalance() {
        Account account = Account.open(1, "1002-111-111111","입출금");
        account.deposit(new BigDecimal("10000"));
        assertThatCode(() -> account.withdraw(new BigDecimal("10000"))).doesNotThrowAnyException();
        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }
    
}