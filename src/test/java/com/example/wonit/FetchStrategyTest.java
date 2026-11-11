package com.example.wonit;


import com.example.wonit.model.*;
import com.example.wonit.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// LAZY · EAGER · N+1 · @EntityGraph 를 "SQL 이 몇 번 나갔나" 로 확인하는 테스트
// 실행: ./gradlew test --tests '*FetchStrategyTest'  (MySQL 필요 없음. h2 프로필 + 시드 데이터: 거래 7건, 주인 계좌 3개)
// 콘솔의 org.hibernate.SQL 줄과 "SQL n번" 로그를 같이 보세요.
//
// 🔎 실험: Transaction.account 의 fetch = FetchType.LAZY 를 EAGER 로 바꾸고 다시 돌려 보세요.
//    ① lazy_accountIsLoadedWhenUsed : 꺼낸 직후 이미 계좌가 채워져 있어서(isInitialized=true) 실패한다
//    ② nPlusOne_withoutEntityGraph  : 계좌를 쓰지도 않았는데 findAll() 만으로 계좌 SELECT 가 따라 나가 실패한다
//    ③ entityGraph_oneQuery          : 그대로 통과 (JOIN 한 번)
//    확인이 끝나면 LAZY 로 되돌립니다.
@Slf4j
@SpringBootTest
@ActiveProfiles("h2")
@Transactional   // 🚨 LAZY 로 미뤄 둔 계좌를 나중에 가지러 가려면 DB 연결(영속성 컨텍스트)이 열려 있어야 한다. 없으면 LazyInitializationException
class FetchStrategyTest {

    @Autowired
    TransactionRepository transactionRepository;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;

    Statistics stats;

    @BeforeEach
    void setUp() {
        // Hibernate 가 실행한 SQL 개수를 세는 계수기. 켜고 0 으로 맞춘다
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        startCounting();
    }

    @Test
    @DisplayName("LAZY: 거래만 먼저 SELECT, 계좌는 getAccountNo() 를 부르는 순간 SELECT")
    void lazy_accountIsLoadedWhenUsed() {
        Transaction tx = transactionRepository.findById(1L).orElseThrow();
        assertThat(sqlCount("거래 1건 꺼냄")).isEqualTo(1);
        assertThat(Hibernate.isInitialized(tx.getAccount())).isFalse();   // 계좌 자리에는 아직 "대리인(프록시)" 만 있다

        String accountNo = tx.getAccount().getAccountNo();                // ← 여기서 account SELECT
        assertThat(accountNo).isEqualTo("1002-345-678901");
        assertThat(sqlCount("계좌번호 읽음")).isEqualTo(2);
    }

    @Test
    @DisplayName("N+1: @EntityGraph 없는 findAll() 후 거래마다 계좌를 읽으면 SQL = 1 + 계좌 수")
    void nPlusOne_withoutEntityGraph() {
        List<Transaction> txs = transactionRepository.findAll();          // JpaRepository 기본 메서드 (@EntityGraph 없음)
        assertThat(sqlCount("거래 목록")).isEqualTo(1);                   // LAZY 라서 계좌는 아직 안 읽었다

        long accounts = txs.stream().map(tx -> tx.getAccount().getAccountNo()).distinct().count();   // 거래를 돌며 계좌번호를 읽는다
        assertThat(txs).hasSize(7);
        assertThat(accounts).isEqualTo(3);
        assertThat(sqlCount("거래마다 계좌번호 읽음")).isEqualTo(1 + 3);   // 목록 1번 + 계좌 3번 (같은 계좌는 한 번 읽으면 기억해 둔다)
    }

    @Test
    @DisplayName("@EntityGraph: 계좌까지 JOIN 으로 한 번에 → 계좌를 읽어도 SQL 1번")
    void entityGraph_oneQuery() {
        List<Transaction> txs = transactionRepository.findAllByOrderByTxDatetimeDescTxIdDesc();   // @EntityGraph(attributePaths = "account")
        txs.forEach(tx -> tx.getAccount().getAccountNo());
        assertThat(txs).hasSize(7);
        assertThat(sqlCount("JOIN 으로 거래 + 계좌")).isEqualTo(1);
    }

    // 1차 캐시(영속성 컨텍스트)를 비워서, 앞에서 읽어 둔 계좌가 재사용되지 않게 한다. 그리고 계수기를 0 으로
    private void startCounting() {
        em.flush();
        em.clear();
        stats.clear();
    }

    private long sqlCount(String step) {
        long count = stats.getPrepareStatementCount();
        log.debug("[{}] 지금까지 SQL {}번", step, count);
        return count;
    }
}