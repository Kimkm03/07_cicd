package com.example.wonit.repository;

import com.example.wonit.model.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // "spring.sql.init.mode=never"                    // main 의 data.sql(MySQL 용 시드)을 H2 에 넣지 않는다
})
class AccountRepositoryTest {

    @Autowired
    AccountRepository accountRepository;

    @Test
    @DisplayName("계좌번호로 계좌를 찾는다")
    void findByAccountNo() {
        // 1002-345-678901는 data.sql에 이미 있는 계좌이므로 에러
//        Account saved = accountRepository.save(Account.open(1, "1002-345-678901", "입출금"));
         Account saved = accountRepository.save(Account.open(10, "110-11-111111", "입출금"));

        Optional<Account> found = accountRepository.findByAccountNo(saved.getAccountNo());

        assertThat(found).isPresent();
        assertThat(found.get().getBalance()).isEqualByComparingTo("0.00");
    }   // @DataJpaTest 는 테스트가 끝나면 자동으로 롤백해 준다 → DB 가 깨끗하게 돌아온다
}