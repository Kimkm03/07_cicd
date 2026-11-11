# 참고: LAZY · EAGER · N+1 · @EntityGraph · 순환 참조

> 근거 코드: `Transaction.java` · `TransactionRepository.java` · `src/test/java/com/example/wonit/FetchStrategyTest.java` · `LazyLoadingTryTest.java`
> 실행: `./gradlew test --tests '*FetchStrategyTest'` (H2 메모리 DB, 시드 거래 7건 · 계좌 3개)

## 1. 한 줄 정리

| 용어 | 뜻 |
|---|---|
| LAZY | 필요할 때 가지러 간다. `getAccount().getAccountNo()` 를 부르는 **순간** 계좌 SELECT |
| EAGER | 일단 다 가져온다. 계좌를 **안 써도** 같이 읽는다 |
| N+1 | 목록 SELECT **1번** + 항목마다 연관 SELECT **N번** |
| `@EntityGraph` | "이 조회에서만 연관 엔티티도 JOIN 으로 같이 가져와" |

## 2. 우리 코드 (`Transaction.java`)

```java
@ManyToOne(fetch = FetchType.LAZY)               // 🚨 @ManyToOne 의 기본값은 EAGER. 꼭 LAZY 로 바꾼다
@JoinColumn(name = "account_id")                 // 이 칸이 FK
private Account account;
```

```java
// TransactionRepository.java
@EntityGraph(attributePaths = "account")        // 칸 이름(account_id)이 아니라 엔티티 "필드 이름"(account)
List<Transaction> findAllByOrderByTxDatetimeDescTxIdDesc();
```

## 3. 테스트가 보여 주는 것 (`FetchStrategyTest`)

테스트는 Hibernate 통계(`getPrepareStatementCount`)로 **SQL 이 몇 번 나갔는지** 셉니다. 클래스에 `@Transactional` 이 붙어 있어야 LAZY 로 미룬 계좌를 나중에 가지러 갈 수 있습니다.

| 테스트 | 하는 일 | SQL 횟수 |
|---|---|---|
| `lazy_accountIsLoadedWhenUsed` | `findById(1L)` 후 계좌번호 읽기 | 거래 꺼냄 **1** → 계좌번호 읽음 **2**. 꺼낸 직후 `Hibernate.isInitialized(tx.getAccount())` 는 `false` (대리인 프록시만 있음) |
| `nPlusOne_withoutEntityGraph` | `findAll()` 후 거래마다 계좌번호 읽기 | 목록 **1** → 읽은 뒤 **1 + 3 = 4** (거래 7건, 계좌 3개. 같은 계좌는 한 번 읽으면 기억해 둔다) |
| `entityGraph_oneQuery` | `@EntityGraph` 붙은 메서드로 조회 후 계좌번호 읽기 | **1** (거래 LEFT JOIN 계좌) |

> 핵심: N+1 의 N 은 "거래 수" 가 아니라 **서로 다른 계좌 수**(3)입니다. 영속성 컨텍스트(1차 캐시)가 이미 읽은 계좌를 재사용하기 때문이에요. 그래서 테스트 시작 때 `em.flush(); em.clear();` 로 캐시를 비웁니다.

`LazyLoadingTryTest` 는 같은 LAZY 를 가장 단순하게 보는 테스트입니다. 거래를 꺼낸 로그 다음에 계좌번호 로그가 나오는 사이에 SELECT 가 따로 찍힙니다. 클래스의 `@Transactional` 을 지우면 `getAccount()` 에서 `LazyInitializationException` (DB 연결이 이미 닫힘)이 납니다.

## 4. 실험: EAGER 로 바꿔 보기

`FetchStrategyTest` 파일 맨 위 주석에 적힌 실험입니다. `Transaction.account` 의 `LAZY` 를 `EAGER` 로 바꾸고 같은 테스트를 다시 돌립니다.

| 테스트 | 결과 | 이유 |
|---|---|---|
| `lazy_accountIsLoadedWhenUsed` | 실패 | 꺼내자마자 계좌가 채워져 `isInitialized` 가 `true` |
| `nPlusOne_withoutEntityGraph` | 실패 | 계좌를 안 썼는데 `findAll()` 만으로 계좌 SELECT 가 따라 나감 |
| `entityGraph_oneQuery` | 통과 | 어차피 JOIN 한 번 |

**확인이 끝나면 반드시 `LAZY` 로 되돌립니다.**

> 정리: EAGER 는 필요 없는 순간에도 읽고, 목록 조회에서 N+1 을 숨깁니다. 그래서 연관관계는 LAZY 로 두고, 같이 필요한 조회에만 `@EntityGraph` 를 붙입니다.

## 5. 실험: `@EntityGraph` 를 빼 보기

`TransactionRepository` 의 `@EntityGraph(...)` 줄을 지우고 같은 테스트를 돌리면, JOIN 한 번이던 조회가 `1 + 계좌 수` 로 늘어납니다. (저장소 주석에도 같은 확인 방법이 적혀 있습니다.) 확인 뒤 원래대로 돌려 놓습니다.

## 6. 순환 참조 (학생이 임시로 추가해 보는 실험)

> 🚨 지금 코드에는 **없는** 상황입니다. 아래는 임시로 추가했다가 **반드시 되돌립니다.**

현재 연관관계는 **거래 → 계좌** 한 방향뿐입니다. `Account.java` 에는 아래 코드가 **주석 처리**된 채 남아 있고, 주석에도 `StackOverflowError 방지` 라고 적혀 있습니다.

```java
//    @OneToMany(mappedBy = "account")
//    private List<Transaction> transactions;
```

두 클래스 모두 `@Data` 라서 `toString()` 이 필드를 전부 찍습니다. 주석을 풀어 **계좌 → 거래** 방향을 만들면 고리가 생깁니다.

```
Account.toString() → transactions 의 각 Transaction.toString() → account 의 Account.toString() → ...  (끝없음)
```

**해보기**

1. `Account` 의 위 두 줄 주석을 푼다 (필요하면 `ArrayList` 로 초기화)
2. 테스트에서 `accountRepository.findByAccountNo("1002-345-678901")` 로 꺼낸 계좌에 `account.toString()` (또는 `log.debug("{}", account)`) 를 호출한다
3. `StackOverflowError` 를 확인한다
4. `transactions` 위에 `@ToString.Exclude` 를 붙이면 사라지는 것을 확인한다
5. **주석을 다시 달고 임시 테스트를 지운다**

엔티티를 컨트롤러에서 그대로 JSON 으로 반환하면 JSON 변환기도 같은 고리를 따라가다 오류가 날 수 있습니다. 응답은 DTO 로 내보내는 것이 기본입니다. (이 JSON 쪽은 코드로 확인하지 않았습니다.)

| 해결 | 설명 |
|---|---|
| 단방향만 쓴다 | 지금 코드의 선택. 계좌의 거래는 `findByAccount_AccountNo(accountNo, pageable)` 로 조회 |
| `@ToString.Exclude` | 양방향이 꼭 필요할 때 한쪽에 붙인다 |
| 응답은 DTO | 엔티티를 직접 내보내지 않는다 |

## 🎤 면접

<details>
<summary>Q. LAZY 와 EAGER 의 차이, 그리고 N+1 은 어떻게 해결하나요?</summary>

"EAGER 는 연관 엔티티를 항상 같이 읽고, LAZY 는 실제로 쓸 때 읽습니다. 저희 프로젝트에서 거래 7건 · 계좌 3개를 `findAll()` 로 읽고 계좌번호를 쓰면 SQL 이 1+3=4번 나가는 N+1 을 테스트로 확인했습니다. 연관관계는 LAZY 로 두고 같이 필요한 조회에만 `@EntityGraph` 를 붙여 JOIN 한 번(SQL 1번)으로 줄였습니다. LAZY 는 트랜잭션 밖에서 읽으면 LazyInitializationException 이 나므로 트랜잭션 안에서 필요한 값을 읽어 DTO 로 내보냅니다."

</details>

<details>
<summary>Q. 양방향 연관관계에서 순환 참조를 어떻게 피하나요?</summary>

"`@Data` · `@ToString` 이 양쪽에 있으면 `toString` 이 서로를 부르다 StackOverflowError 가 납니다. 저희는 거래 → 계좌 단방향만 두고, 양방향이 필요하면 한쪽에 `@ToString.Exclude` 를 붙이며, 응답은 DTO 로 내보냅니다."

</details>
