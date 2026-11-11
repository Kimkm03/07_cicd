package com.example.wonit;

import com.example.wonit.model.Transaction;
import com.example.wonit.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest
@ActiveProfiles("h2")   // H2 + 데모 데이터. 1번 거래 = 1번 계좌(1002-345-678901)의 거래
@Transactional          // 없으면 getAccount() 에서 LazyInitializationException
class LazyLoadingTryTest {
    @Autowired
    TransactionRepository transactionRepository;

    @Test
    void lazy() {
        Transaction tx = transactionRepository.findById(1L).orElseThrow();
        log.debug("거래를 꺼냈다 txId={}", tx.getTxId());
        log.debug("계좌번호={}", tx.getAccount().getAccountNo());
    }
}