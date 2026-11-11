package com.example.wonit.repository;

import com.example.wonit.dto.CategorySpending;
import com.example.wonit.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // 전체 거래를 최신순으로 (메서드 이름이 곧 쿼리: findAll + OrderBy TxDatetime + Desc)
    //
    // @EntityGraph = "이 조회에서만은 account 도 같이 가져와"
    //   - Transaction.account 는 LAZY 라서, 평소에는 거래만 SELECT 하고 계좌는 getAccount() 를 쓰는 순간 따로 SELECT 한다
    //   - 그래서 거래 목록을 돌며 계좌번호를 읽으면 "목록 1번 + 계좌마다 1번" = N+1 번 SQL 이 나간다
    //   - @EntityGraph 를 붙이면 처음부터 거래 LEFT JOIN 계좌 로 한 번에 가져온다 → 계좌를 읽어도 SQL 1번
    //   - attributePaths 에는 DB 칸 이름(account_id)이 아니라 엔티티의 "필드 이름"(account)을 적는다
    //
    // 🚨 엔티티의 LAZY 는 그대로 두고, "같이 필요한 조회" 에만 이렇게 지정한다
    //    (엔티티를 EAGER 로 바꾸면 계좌가 필요 없는 조회까지 전부 계좌를 끌고 온다)
    // 🔎 확인: 이 줄을 지우고 FetchStrategyTest 를 돌리면 SQL 이 1번 → 1 + 계좌 수 로 늘어난다
    @EntityGraph(attributePaths = "account")
    List<Transaction> findAllByOrderByTxDatetimeDescTxIdDesc();

    List<Transaction> findByCategoryOrderByTxDatetimeDescTxIdDesc(String category);

    // JPQL: 쿼리를 하이버네이트가 만들었는데 동작이 비효율적이거나 JPA의 규칙으로 만들기 어려운 쿼리인 경우
    // 직접 입력값만 함수 시그니처로 정의해놓고 위에 해당 변수들을 받아서 사용할 쿼리를 작성해줍니다.

    @Query("""
            select t from Transaction t
            where (:category is not null and t.category = :category)
               or (:memo is not null and lower(t.memo) like lower(concat('%', :memo, '%')))
            order by t.txDatetime desc, t.txId desc
            """)
    List<Transaction> searchByCategoryOrMemo(
            @Param("category") String category,
            @Param("memo") String memo
    );

    List<Transaction> findByAmountBetweenOrderByTxDatetimeDescTxIdDesc(
            long minAmount,
            long maxAmount
    );



    Page<Transaction> findByAccount_AccountNo(String accountNo, Pageable pageable);

    // 📊 리포트: 숫자는 DB 가 계산한다 (group by 로 카테고리별 합계 · 건수). 🔐 customerId 로 남의 거래는 빼고
    @Query("""
            select new com.example.wonit.dto.CategorySpending(t.category, sum(t.amount), count(t))
            from Transaction t
            where t.account.customerId = :customerId and t.txType = '출금'
              and t.txDatetime >= :from and t.txDatetime < :to
            group by t.category
            order by sum(t.amount) desc
            """)
    List<CategorySpending> spendingByCategory(@Param("customerId") Integer customerId,
                                              @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 월별 합계용: 이 날짜부터의 내 거래 (계좌를 같이 가져와 N+1 을 막는다)
    @EntityGraph(attributePaths = "account")
    List<Transaction> findByAccount_CustomerIdAndTxDatetimeGreaterThanEqual(Integer customerId, LocalDateTime from);
}