/**
 * JavaDoc - 함수나 클래스에 대한 설명을 적어놓으면
 */

// 한 줄 주석

/*
여러 줄 주석
 */
package com.example.wonit.service;

import com.example.wonit.dto.AccountCreateRequest;
import com.example.wonit.dto.AccountPatchRequest;
import com.example.wonit.dto.DepositRequest;
import com.example.wonit.dto.TransferRequest;
import com.example.wonit.dto.TransactionResponse;
import com.example.wonit.exception.*;
import com.example.wonit.model.Account;
import com.example.wonit.model.Transaction;
import com.example.wonit.repository.AccountRepository;
import com.example.wonit.repository.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j // 로그를 이 클래스에서 사용하겠다라는 어노테이션
// Controller와만 통신하며 요청에 대한 실제 로직을 담당
@Service
@RequiredArgsConstructor
public class AccountService {


    @Autowired // 서로서로 연결된 bean임을 알려줍니다. 생략하면 Springboot가 넣어줌
    private final AccountRepository accountRepository;

    // TransactionRepository를 주입받아야 합니다.
    private final TransactionRepository transactionRepository;

    // 도우미 메서드 (withdraw · deposit · transfer 가 함께 쓴다. 잠금 조회를 한 곳에 모은다)
    // getAccount() 대신 lockAccount() 를 쓴다
    private Account lockAccount(String accountNo) {
        return accountRepository.findByAccountNoForUpdate(accountNo)
                .orElseThrow(() -> new AccountNotFoundException(accountNo + " 계좌를 찾을 수 없습니다."));
    }

    // 모든 계좌를 리턴
    public List<Account> findAll() {

        // 코드1
        List<Account> accounts =  accountRepository.findAll();

        // 로깅을 위해서 객체를 안에서 만들어 놓고
        log.info("전체 계좌 조회 건수 출력={}", accounts.size());
        log.info("전체 계좌 조회 건수 개별 출력={}", accounts.toString());
        // 리턴함

        // 코드3: 로그는 return이 일어나기 전에 보통 찍습니다.
        return accounts;
    }

    // 🔐 로그인한 고객의 계좌만
    public List<Account> findMine(Integer customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    // 🔐 주소에 적힌 계좌가 토큰 주인의 계좌인지 확인한다. 남의 계좌면 403 (없는 계좌면 getAccount 가 404)
    public void checkMine(Integer customerId, String accountNo) {
        if (!customerId.equals(getAccount(accountNo).getCustomerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 계좌만 사용할 수 있습니다.");
        }
    }

    // 하나의 계좌 정보를 repo에서 받아서 리턴하거나 NPE을 막아주는 함수
    public Optional<Account> findOne(String accountNo) {
        return accountRepository.findByAccountNo(accountNo);
    }


    // 하나의 계좌 정보를 리턴 -> 받아서 Account 자료형으로 받어나 예외를 일으키는 함수

    /**
     * Controller에 특정 계좌번호의 검색 결과 단건을 반환한다.
     *
     * @param accountNo
     * @return Account
     */
    public Account getAccount(String accountNo) {
        log.info("계좌 조회={}", accountNo);
        return findOne(accountNo).orElseThrow(() -> new AccountNotFoundException(accountNo + "계좌를 찾을 수 없습니다"));
    }

    // 실제동작을 아래 함수에 작성해 줍니다. db에서 모든 계좌를 조회해서
    public List<String> normalAccountNos() {
        return accountRepository.findAll()
                .stream().filter(account -> account.getStatus().equals("정상"))
                .map(Account::getAccountNo).toList();  // .mapToLong(Account::getBalance).sum();
    }

    //    public long savingBalanceSum() {
    public BigDecimal savingBalanceSum() {
            return accountRepository.findAll()
                    .stream().filter(account -> account.getAccountType().equals("적금"))
    //                .mapToLong(Account::getBalance).sum();
                    .map(Account::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }


    public List<Account> findAllAccountsByBalanceAsc() {
        return accountRepository.findAllByOrderByBalanceAsc();
    }

    public List<Account> findAllAccountsByStatus(String status) {
        return accountRepository.findByStatusContains(status); // 일부일치
        // return accountRepository.findByStatusLike(status); // 완전일치
    }
    public List<Account> findAllAccountsByAccountType(String accountType) {
        return accountRepository.findByAccountTypeLike(accountType);
    }


    public List<Account> getAccountBetweenBalance(long minBalance, long maxBalance) {
        log.debug("Repository 잔액 조회 시작: minBalance={}, maxBalance={}",
                minBalance, maxBalance);

        List<Account> accounts =
                accountRepository.findByBalanceBetween(minBalance, maxBalance);

        log.debug("Repository 잔액 조회 완료: resultCount={}", accounts.size());
        return accounts;
    }

    public Account createAccount(AccountCreateRequest request) {
        log.debug("계좌 생성 검증 시작: customerId={}, accountType={}",
                request.customerId(), request.accountType());

        if (request.customerId() == null || request.customerId() <= 0) {
            throw new InvalidAccountRequestException("올바른 customerId가 필요합니다.");
        }

        if (!List.of("입출금", "적금").contains(request.accountType())) {
            throw new InvalidAccountRequestException("지원하지 않는 accountType입니다.");
        }

        String accountNo;
        do {
            accountNo = java.util.UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 20);
        } while (accountRepository.existsByAccountNo(accountNo));

        log.debug("계좌 저장 시작: customerId={}, accountType={}",
                request.customerId(), request.accountType());

        Account account = accountRepository.save(
                Account.open(request.customerId(), accountNo, request.accountType())
        );

        log.debug("계좌 저장 완료: accountId={}", account.getAccountId());
        return account;
    }

    // 10.19 수업 시작
//    public Account updateOne(String accountNo, AccountCreateRequest request) {
//        // 원본 가져오기
//        // Account account = getAccount(accountNo);
//        Account account = lockAccount(accountNo);
//
//        // 화면에 뿌리기 (통째로 바꾸는 경우에는 생략 가능 / 일부만 가져올 경우에는)
//        account.setCustomerId(request.customerId());
//        account.setAccountType(request.accountType());
//        // 업무 메서드를 하나 Entity에 작성해서 변경
//
//        //  - 원본의 내용과 일치하는 필드인지 -> 일치하면 변경, 그렇지 않으면 스킵
//        // 바뀐 내용 새로 객체로 저장하기
//        // DB에 반영하기(레파지토리)
//        Account saved = accountRepository.save(account);
//        // 반영된 상태 확인하기
//        return account;
//    }

    // lockAccount() 는 잠금 조회(SELECT ... FOR UPDATE)라서 트랜잭션 안에서만 동작합니다.
    @Transactional
    public Account updateOne(String accountNo, AccountCreateRequest request) {
        // 원본 가져오기
//        Account account = getAccount(accountNo);
        Account account = lockAccount(accountNo);


        // 화면에 뿌리기 (통째로 바꾸는 경우에는 생략 가능 / 일부만 가져올 경우에는)
        account.setCustomerId(request.customerId());
        account.setAccountType(request.accountType());
        // 업무 메서드를 하나 Entity에 작성해서 변경

        //  - 원본의 내용과 일치하는 필드인지 -> 일치하면 변경, 그렇지 않으면 스킵
        // 바뀐 내용 새로 객체로 저장하기
        // DB에 반영하기(레파지토리) -- @Transactional이 붙어있으면 save()를 직접 호출하지 않아도 변경 감지가 일어나서 DB에 반영됩니다.
//        Account saved = accountRepository.save(account);
        // 반영된 상태 확인하기
        return account;
    }

    // 상태 변경 (PATCH): 변경 감지가 없으므로 save() 를 직접 부른다
    // PUT은 “통째로 바꾸기”, PATCH는 “필드별 부분 수정”

    // lockAccount() 는 잠금 조회(SELECT ... FOR UPDATE)라서 트랜잭션 안에서만 동작합니다.
    @Transactional
    public Account patchAccount(String accountNo, AccountPatchRequest request) {
        // 원본 가져오기
//        Account account = getAccount(accountNo);
        Account account = lockAccount(accountNo);
        //  - 원본의 내용과 일치하는 필드인지 -> 일치하면 변경, 그렇지 않으면 스킵
        // customerId나 accountType, status 중 일부만 들어올 수도 있고, 일부는 null일 수도 있음
        // 그래서 if (request.xxx() != null) 로 조건 분기
        if (request.accountType() != null) {
            if (!List.of("입출금", "적금").contains(request.accountType())) {
                throw new InvalidAccountRequestException("지원하지 않는 accountType입니다.");
            }
            account.setAccountType(request.accountType());
        }

        if (request.status() != null) {
            if ("정상".equals(request.status())) {
                account.setStatus("정상");
            } else if ("휴면".equals(request.status())) {
                account.setStatus("휴면");
            } else if ("지급정지".equals(request.status())) {
                account.setStatus("지급정지");
            } else {
                throw new InvalidAccountRequestException("상태는 정상 · 휴면 · 지급정지 중 하나여야 합니다.");
            }
        }

        // 바뀐 내용 새로 객체로 저장하기
        return accountRepository.save(account);
        }

    //  @Transactional은 “이 메서드 안의 DB 작업들을 하나의 트랜잭션으로 묶어줘” 라는 표시입니다.
    // 왜 필요한가?
    //    DB는 원자성을 보장해야 함
    //    중간에 실패하면 전부 롤백되게 해야 함
    //    여러 쿼리를 하나의 작업처럼 묶어야 함
    //    @Transactional이 있으면 전체를 커밋하거나 롤백해줌
    //    save(), delete(), findById() 같은 JpaRepository 기본 메서드는 Spring이 내부적으로 트랜잭션을 관리해줍니다.
    //    하지만 deleteByAccountNo() 같은 커스텀 delete query는 “직접 만든 쿼리”라서 트랜잭션이 불안정해질 수 있습니다.
    // 실습: 현재 Service의 updateOne 메서드를 @Transactional로 바꾸고, save()를 직접 호출하지 않고 변경 감지로 DB에 반영되도록 수정해보세요.
        @Transactional
        public void deleteOneByAccountNo(String accountNo) {
            accountRepository.deleteByAccountNo(accountNo);
        }

        public void deleteOne(String accountNo) {
//            Account account = getAccount(accountNo); // 조회 - 조회가 안되면 예외 발생
            Account account = lockAccount(accountNo);
            accountRepository.delete(account); // 삭제 - 기본 delete 메서드
        }



//  @Transactional은 “이 메서드 안의 DB 작업들을 하나의 트랜잭션으로 묶어줘” 라는 표시입니다.
// 왜 필요한가?
//    DB는 원자성을 보장해야 함
//    중간에 실패하면 전부 롤백되게 해야 함
//    여러 쿼리를 하나의 작업처럼 묶어야 함
//@Transactional
//public void transfer() {
//    // A 출금
//    // B 입금
//}
//    이런 로직에서 중간에 에러가 나면
//    출금만 되고 입금이 안 되는 상황을 막아야 함
//    @Transactional이 있으면 전체를 롤백해줌
//    save(), delete(), findById() 같은 JpaRepository 기본 메서드는 Spring이 내부적으로 트랜잭션을 관리해줍니다.
//    하지만 deleteByAccountNo() 같은 커스텀 delete query는 “직접 만든 쿼리”라서 트랜잭션이 불안정해질 수 있습니다.

//    스레드는 실행 흐름 단위
//    트랜잭션은 요청 단위로 잡히는 경우가 많음
//    Spring은 보통 각 HTTP 요청을 하나의 스레드로 처리
//    그래서 같은 요청 안에서 @Transactional이 붙은 메서드가 실행되면
//    그 스레드 안에서 트랜잭션이 시작되고
//    끝나면 commit/rollback이 됨

    // lockAccount() 는 잠금 조회(SELECT ... FOR UPDATE)라서 트랜잭션 안에서만 동작합니다.
    // 🏦 입금: 잠그고 → 잔액 올리고 → 거래내역 남기기. 셋이 한 묶음(@Transactional)이라 하나라도 실패하면 전부 취소
    @Transactional
    public Transaction deposit(String accountNo, DepositRequest request) {
        Account account = lockAccount(accountNo);
        account.deposit(request.amount());
        return transactionRepository.save(Transaction.of(account, "입금", request.amount(), request.memo(), null));
    }

    // 🏦 이체: 출금 + 입금 + 거래내역 2건이 한 묶음. 도중에 예외(휴면 · 잔액부족)가 나면 출금까지 rollback 된다
    @Transactional
    public Transaction transfer(Integer customerId, TransferRequest request) {
        String fromNo = request.fromAccountNo();
        String toNo = request.toAccountNo();
        if (fromNo.equals(toNo)) {
            throw new InvalidAccountRequestException("같은 계좌로는 이체할 수 없습니다.");   // 400
        }
        // 🚨 데드락 방지: 두 계좌를 항상 계좌번호가 작은 쪽부터 잠근다 (A→B 와 B→A 가 동시에 와도 서로를 기다리지 않는다)
        Account from, to;
        if (fromNo.compareTo(toNo) < 0) { from = lockAccount(fromNo); to = lockAccount(toNo); }
        else                            { to = lockAccount(toNo);     from = lockAccount(fromNo); }

        if (!customerId.equals(from.getCustomerId())) {     // 남의 계좌에서 돈을 빼갈 수 없다 (토큰의 고객번호와 비교)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 계좌에서만 이체할 수 있습니다.");
        }
        from.withdraw(request.amount());
        to.deposit(request.amount());
        Transaction out = transactionRepository.save(Transaction.of(from, "출금", request.amount(), request.memo(), to.getAccountNo()));
        transactionRepository.save(Transaction.of(to, "입금", request.amount(), request.memo(), from.getAccountNo()));
        return out;
    }

    @Transactional
    public Page<Transaction> getTransactions(String accountNo, Pageable pageable) {
//        getAccount(accountNo);
        lockAccount(accountNo);
        return transactionRepository.findByAccount_AccountNo(accountNo, pageable);
    }
}
