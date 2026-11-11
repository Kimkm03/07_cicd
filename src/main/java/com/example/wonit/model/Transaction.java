package com.example.wonit.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


// @Table(name = "`transaction`")      // 🚨 transaction 은 SQL 키워드라 백틱으로 감싸는 게 안전
// @Getter
// @NoArgsConstructor(access = AccessLevel.PROTECTED)
@Data
@Entity
public class Transaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tx_id")
    private Long txId;


    // LAZY (지금 코드): 거래만 먼저 읽고, tx.getAccount().getAccountNo() 처럼 계좌를 실제로 쓰는 순간에 계좌를 SELECT 합니다.
    // EAGER (기본값): 거래를 읽을 때 계좌도 무조건 같이 읽습니다. 계좌를 안 써도 읽습니다.
//     @ManyToOne
    @ManyToOne(fetch = FetchType.LAZY)               // 🚨 @ManyToOne 의 기본값은 EAGER. 꼭 LAZY 로 바꾼다
    @JoinColumn(name = "account_id")                 // 이 칸이 FK
    private Account account;

    private String transferId;          // 송금이면 상대방 계좌번호, 입금이면 상대방 계좌번호, 출금이면 null
    @Column(name = "tx_type")       private String txType;          // "출금" · "입금"
    @Column(name = "amount", precision = 19, scale = 2)        private BigDecimal amount;
    @Column(name = "balance_after", precision = 19, scale = 2) private BigDecimal balanceAfter;
    @Column(name = "category")      private String category;
    @Column(name = "memo")          private String memo;
    @Column(name = "counterparty")  private String counterparty;
    @Column(name = "tx_datetime")   private LocalDateTime txDatetime;

    // 🏦 "출금했다" 라는 업무 이름으로 만든다. 호출하는 쪽이 balanceAfter 를 계산해서 넣는 게 아니라,
    //    account.withdraw() 가 끝난 뒤의 잔액을 그대로 받아 적는다 (장부를 한 번만 계산하기 위해)
    public static Transaction withdrawal(Account account, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.account = account;
        tx.txType = "출금";
        tx.amount = amount;
        tx.balanceAfter = account.getBalance();
        tx.txDatetime = LocalDateTime.now();
        return tx;
    }
    // 🏦 입금 · 이체 출금/입금 공통. account 는 이미 잔액이 바뀐 뒤여야 한다 (balanceAfter = 바뀐 뒤 잔액)
    public static Transaction of(Account account, String txType, BigDecimal amount, String memo, String counterparty) {
        Transaction tx = new Transaction();
        tx.account = account;
        tx.txType = txType;
        tx.amount = amount;
        tx.balanceAfter = account.getBalance();
        tx.memo = memo;
        tx.counterparty = counterparty;
        tx.category = counterparty != null ? "이체" : null;    // 상대 계좌가 있으면 이체
        tx.txDatetime = LocalDateTime.now();
        return tx;
    }
    // transferOut(...) · transferIn(...) 은 6절에서, idempotencyKey 칸은 10절에서
}