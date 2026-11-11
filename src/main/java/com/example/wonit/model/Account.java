package com.example.wonit.model;

import com.example.wonit.exception.AccountDormantException;
import com.example.wonit.exception.AccountFrozenException;
import com.example.wonit.exception.BalanceRemainingException;
import com.example.wonit.exception.InsufficientBalanceException;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;


@Slf4j
//// 보안이 중요한 데이터 처리에는 setter를 직접 만들어서 사용합니다.
//@Getter
//@ToString
//@EqualsAndHashCode
//@RequiredArgsConstructor
@Data // @Getter, @Setter, @ToString, @EqualsAndHashCode, @RequriedArgsConstructor
@Entity
public class Account {

    @Id // Primary Key(고유값을 의미하는 컬럼)을 지정해줘야 이 컬럼을 기반으로 쿼리들이 만들어집니다.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer accountId;
    private Integer customerId;
    private String accountNo;
    private String accountType;
    private String status;
//    private long balance;

    // 🏦 돈은 BigDecimal. 표의 DECIMAL(19,2) 와 같은 모양(전체 19자리 중 소수점 아래 2자리)으로 맞춘다
    @Column(name = "balance", precision = 19, scale = 2)
    private BigDecimal balance;

//    // @OneToMany(mappedBy = "account") 은 Transaction 엔티티에서 account 필드가 이 Account 엔티티와 연관되어 있음을 나타냅니다.
//    // StackOverflowError 방지: @ToString.Exclude 를 사용하여 순환 참조를 피합니다.
//    @OneToMany(mappedBy = "account")
//    private List<Transaction> transactions;

    // @Setter 대신에 우리가 만든 별도의 status가 지급정지는 아닌지, 잔액이 불충분하지는 않은지 확인하는 예외를 만들어서 처리
    // 10000원 넣으면서 통장 만들어주세요
    // 0, 10000 입금 메서드로 처리합니다.
    // 이유: 보안 때문에, 감사 때문에 기록을 남깁니다.
    public static Account open(Integer customerId, String accountNo, String accountType) {
        Account account = new Account();
        account.customerId = customerId;
        account.accountNo = accountNo;
        account.accountType = accountType;
        account.status = "정상";
        // account.balance = 0;
        account.balance = new BigDecimal("0.00");   // DECIMAL(19,2) 와 같은 소수 2자리로 시작 (응답이 0 이 아니라 0.00 으로 나간다)
        return account;
    }

    // 🏦 Soft Delete 해지. 금융 데이터는 지우지 않고 상태만 바꾼다. 거래 기록은 법적으로 수년간 보관해야 한다
    public void close() {
        if (balance.compareTo(BigDecimal.ZERO) != 0) {   // 🚨 금액이 0인지는 equals 가 아니라 compareTo (0 과 0.00 은 equals 로는 다르다)
            throw new BalanceRemainingException(" 계좌에 잔액 " + balance + "원이 남아 있어 해지할 수 없습니다.");
        }
        this.status = "해지";
    }

    // 🏦 @Setter 는 일부러 안 붙였다. 잔액은 업무메서드인 withdraw() · deposit() 로만 바뀌게 해서
    //    "지급정지 · 잔액부족 · 휴면 검사"를 건너뛰고 잔액을 마음대로 고치지 못하게 막는다.
    // 🚨 BigDecimal 은 "불변 객체" 라서 balance -= amount 같은 연산자를 못 쓴다.
    //    계산 결과는 새 객체로 돌아오므로 반드시 다시 대입한다.
    public void withdraw(BigDecimal amount) {
        // 🧪 Day 2 범행 현장 로그: [%thread] 가 같이 찍히므로 "같은 잔액을 두 스레드가 읽는 장면" 이 보인다 (락이 없을 때)
        //    락을 걸면 잔액이 1,000,000 → 990,000 → 980,000 으로 한 칸씩 내려가며 찍힌다. 운영에서는 DEBUG 를 꺼 둔다
        log.debug("잔액 읽음 {}원 → {}원 출금 시도", balance, amount);
        if (status.equals("지급정지")) {
            throw new AccountFrozenException(
                     "지급정지 계좌라 출금할 수 없습니다.");
        }
        if (balance.compareTo(amount) < 0) {                    // balance < amount 를 이렇게 쓴다
            throw new InsufficientBalanceException(
                    "잔액이 " + amount.subtract(balance) + "원 부족합니다.");
        }
        balance = balance.subtract(amount);                     // 새 객체를 다시 담는다
    }

    // 🏦 휴면 계좌는 입금도 막는다 → 이체 도중 여기서 터지면 @Transactional 이 출금까지 rollback 한다 (롤백 실험)
    public void deposit(BigDecimal amount) {
        if (status.equals("휴면")) {
            throw new AccountDormantException(
                    "휴면 계좌라 입금할 수 없습니다.");   // RuntimeException 상속, 409
        }
        balance = balance.add(amount);
    }
}

