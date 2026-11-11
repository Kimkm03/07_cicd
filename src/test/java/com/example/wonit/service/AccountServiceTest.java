package com.example.wonit.service;

import com.example.wonit.model.Account;
import com.example.wonit.repository.AccountRepository;
import com.example.wonit.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void getAccount_계좌번호로_정상조회() {
        // given
        String accountNo = "1002-111-111111";
        Account account = Account.open(1, accountNo, "입출금");

        given(accountRepository.findByAccountNo(accountNo))
                .willReturn(Optional.of(account));

        // when
        // 실제 서비스 메서드를 호출
        Account result = accountService.getAccount(accountNo);

        // then
        // 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.getAccountNo()).isEqualTo(accountNo);
        assertThat(result.getCustomerId()).isEqualTo(1);
        assertThat(result.getAccountType()).isEqualTo("입출금");

        // 가짜 Repository가 실제로 호출됐는지 확인
        verify(accountRepository).findByAccountNo(accountNo);
    }
}