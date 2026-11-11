# Day 0. 요청 · 돈 · 트랜잭션 · 테스트: PUT · PATCH, DELETE(해지), 페이징, Header · Body, BigDecimal, @Transactional, 비관적 락, JUnit

> 3일 과정에 들어가기 전, **10/15(목) 이후 수업에서 함께 만든 코드(`wonbanking-backend` main)** 에 이미 들어 있는 여덟 가지를 읽고 실행해 봅니다. **코드는 추가하지 않습니다.**
> 시작 코드: 태그 **`t0-start`** (레포의 첫 커밋). `git switch -c my-day0 t0-start` 로 내 브랜치를 만들어 시작합니다.

## 🎯 오늘의 목표

- `PUT` 과 `PATCH` 의 차이를 우리 코드로 설명하고, 칸을 빠뜨린 `PUT` 이 어떻게 되는지 안다
- `DELETE` 로 계좌를 진짜 지우면 왜 안 되는지(FK · 기록 보관) 알고, Soft Delete(상태 = 해지)로 바꾸는 방법을 안다
- 거래내역을 `page` · `size` 로 **한 쪽씩** 받는 방법과, 응답의 `totalPages` · `last` 를 읽는 법을 안다
- 요청의 Header(겉면)와 Body(속지)를 구분하고, `Content-Type` 이 빠지면 왜 415 가 나는지 안다
- 돈을 `double` 이 아니라 `BigDecimal` 로 다루는 이유와, `compareTo` · 불변 객체 규칙을 안다
- `@Transactional` 이 해 주는 두 가지(**한 묶음 · 변경 감지**)를 로그와 응답으로 확인한다
- 비관적 락(`SELECT ... FOR UPDATE`)이 왜 필요하고, 왜 트랜잭션 안에서만 동작하는지 설명한다
- 단위 · 슬라이스 · 통합 테스트의 차이를 우리 테스트 파일로 설명하고, `./gradlew test` 결과 보고서를 연다

> 📌 앱을 먼저 실행하세요: `./gradlew bootRun` (H2 메모리 DB, 시드 계좌 3개 · 거래 7건). 요청은 Swagger(`/swagger-ui.html`)나 아래 Postman 컬렉션으로 보냅니다. (`src/api.http` 는 Day 1 태그 `t1` 부터 있습니다. 이 자료의 `###` 요청 예시는 Postman · Swagger 에 그대로 옮겨 적으면 됩니다)

> 📌 **시드 데이터 한 가지** : 1번 계좌(`1002-345-678901`)는 일부러 잔액(1,523,000)과 마지막 거래의 `balance_after`(1,511,000)가 **12,000원 다릅니다.** `data.sql` 주석에 있는 "비정규화한 칸이 어긋난 사고 현장"입니다. 고치지 않습니다. 거래내역을 볼 때 이 숫자를 알아봐 두세요.

## 🧰 Postman 으로 전체 API 호출하기

`docs/postman/wonbanking.postman_collection.json` 에 그날까지 배운 API 가 Day 별 폴더로 들어 있습니다 (Day 3 까지 모두 58개).

1. Postman → **Import** → 위 JSON 파일을 끌어다 놓습니다.
2. 서버를 켠 뒤, 컬렉션의 요청을 위에서부터 하나씩 **Send** 합니다.
3. 컬렉션 **Variables** 탭의 변수를 씁니다.

| 변수 | 값 | 누가 채우나 |
|---|---|---|
| `host` | `http://localhost:8080` | 처음부터 |
| `accountNo` | `1002-345-112233` (실습 계좌) | 처음부터 |
| `newAccountNo` | 새 계좌번호 | "계좌 개설" 요청이 응답에서 자동 저장 |
| `token` | JWT | "Day 2 › 로그인" 요청이 자동 저장 → 모든 요청에 `Authorization: Bearer {{token}}` 으로 붙는다 |

- 그날 태그에 없는 API 는 **404** 가 정상입니다 (예: Day 0 코드에서 AI 요청).
- **Day 2(`t3`) 부터는 "Day 2 › 로그인" 을 먼저** 실행합니다. 안 하면 모든 요청이 401 입니다.
- "Day 2" 폴더 끝의 "woori 로 로그인" 은 토큰을 woori 것으로 바꿔 **남의 계좌 → 403** 을 확인합니다. 바로 뒤의 "다시 yeonji 로 로그인" 까지 실행해야 Day 3 요청이 yeonji 로 갑니다.
- 요청 이름에 기대 결과를 적어 두었습니다 (예: "415 실험", "DELETE ... 지금 코드: 500 / 고치면 409").

### 🧩 체크포인트

Postman 은 요청을 보내고 **결과를 자동으로 검사**할 수도 있습니다.

1. 아무 요청(예: `GET /api/accounts`)을 열고 **Tests** 탭에 아래를 적은 뒤 Send 하세요. (새 버전 Postman 은 **Scripts → Post-response**) 아래쪽 **Test Results** 에 ✓ 가 뜨는지 봅니다.

```javascript
pm.test("상태코드 200", () => pm.response.to.have.status(200));
pm.test("계좌 3개", () => pm.expect(pm.response.json().length).to.eql(3));    // 시드 그대로일 때
pm.test("X-Request-Id 헤더", () => pm.response.to.have.header("X-Request-Id"));
```

2. 세 번째 검사는 지금(Day 0 코드)은 **실패**할까요, 성공할까요? 이유는?
3. 직접 써 보기: "415 실험" 요청의 Tests 에 415 가 오는지 확인하는 `pm.test` 를 적어 보세요.

<details>
<summary>정답 보기</summary>

2. **실패**합니다. `X-Request-Id` 응답 헤더는 Day 2(`t4`)에서 요청 추적을 붙인 뒤부터 생깁니다. Day 2 코드에서 다시 보내면 ✓ 가 됩니다.
3. `pm.test("415", () => pm.response.to.have.status(415));`

</details>

## ⏱ 오늘의 흐름

| 순서 | 주제 | 우리 코드 | 확인 방법 |
|---|---|---|---|
| 1 | PUT vs PATCH | `AccountController.updateOne` · `changeStatus` | 요청을 보내고 응답 비교 |
| 2 | DELETE (해지) | `AccountController.deleteOne` · `Account.close` | 진짜 삭제(200 · 500) vs Soft Delete(204 · 409) |
| 3 | 페이징 | `AccountController.transactions` · `TransactionRepository.findByAccount_AccountNo` | `?page=0&size=2` 로 쪽 넘기기 |
| 4 | Header · Body | `AccountController.createAccount` | `Content-Type` 빼고 보내기 (415) |
| 5 | BigDecimal | `Account.withdraw` · `deposit` · `open` | jshell 로 계산 비교 |
| 6 | @Transactional | `AccountService.updateOne` · `patchAccount` | SQL 로그 · 롤백 실험 |
| 7 | 비관적 락 | `AccountRepository.findByAccountNoForUpdate` · `AccountService.lockAccount` | SQL 로그의 `for update`, H2 콘솔 두 탭 |
| 8 | JUnit | `src/test` 의 테스트 9개 클래스 | `./gradlew test` · 결과 보고서 |

> 📝 각 절 끝의 🧩 체크포인트와 마지막의 🏁 미션 · ⚡ 퀴즈로 이해했는지 확인합니다 (하루 20~30분)

---

# 1. PUT 과 PATCH

## 🤔 왜 필요할까요?

둘 다 "고치기" 인데 뜻이 다릅니다. **PUT 은 통째로 교체, PATCH 는 보낸 칸만 수정**입니다. 이걸 섞어 쓰면 "상태만 바꾸려다 계좌 종류가 지워지는" 사고가 납니다.

## 💻 우리 코드

```java
// AccountController
@PutMapping("/{accountNo}")                       // PUT /api/accounts/{accountNo}
public AccountResponse updateOne(@PathVariable("accountNo") String accountNo,
                                 @RequestBody AccountCreateRequest request) {   // {customerId, accountType}
    return AccountResponse.from(accountService.updateOne(accountNo, request));
}

@PatchMapping("/{accountNo}/status")              // PATCH /api/accounts/{accountNo}/status
public AccountResponse changeStatus(@PathVariable("accountNo") String accountNo,
                                    @Valid @RequestBody AccountPatchRequest request) {   // {accountType?, status?}
    return AccountResponse.from(accountService.patchAccount(accountNo, request));
}
```

```java
// AccountService.updateOne (PUT): 받은 값을 그대로 덮어쓴다 → 빠진 칸은 null 로 덮인다
account.setCustomerId(request.customerId());
account.setAccountType(request.accountType());

// AccountService.patchAccount (PATCH): null 이 아닌 칸만 바꾼다
if (request.accountType() != null) { ... account.setAccountType(request.accountType()); }
if (request.status() != null)      { ... account.setStatus(...); }
```

| | PUT | PATCH |
|---|---|---|
| 뜻 | 이 자원을 **보낸 내용으로 통째로** 바꿔라 | **보낸 칸만** 바꿔라 |
| 보낼 것 | 고칠 수 있는 칸 **전부** | 바꿀 칸 **만** |
| 빠진 칸 | **null 로 덮인다** (교체니까) | 그대로 둔다 |
| 두 번 보내면 | 같은 결과 (멱등) | 우리 코드처럼 "값으로 바꾸기" 면 같은 결과 (멱등) |
| 우리 코드 | `PUT /api/accounts/{계좌번호}` | `PATCH /api/accounts/{계좌번호}/status` |

## 🪵 실행 · 확인

`1002-345-112233` (적금 · 정상) 으로 해 봅니다.

```http
### ① PUT 전부 보내기
PUT http://localhost:8080/api/accounts/1002-345-112233
Content-Type: application/json

{"customerId":1,"accountType":"입출금"}

### ② PUT 에서 accountType 을 빠뜨리기
PUT http://localhost:8080/api/accounts/1002-345-112233
Content-Type: application/json

{"customerId":1}

### ③ PATCH 로 status 만
PATCH http://localhost:8080/api/accounts/1002-345-112233/status
Content-Type: application/json

{"status":"휴면"}
```

실제 응답 (H2 시드 기준):

```
① {"accountNo":"1002-345-112233","accountType":"입출금","balance":1099500.00,"status":"정상"}
② {"accountNo":"1002-345-112233","accountType":null,  "balance":1099500.00,"status":"정상"}   ← 200 인데 accountType 이 지워졌다
③ {"accountNo":"1002-345-112233","accountType":"적금", "balance":1099500.00,"status":"휴면"}   ← (② 뒤에 적금으로 되돌린 상태에서) status 만 바뀌었다
```

- ② 가 PUT 의 성질입니다. "통째로 교체" 라서 안 보낸 칸은 null 이 됩니다.
- 🚨 `AccountController` 의 PUT 주석에는 "하나라도 빠지면 400" 이라고 적혀 있지만, **지금 코드는 200 이고 null 로 덮습니다.** 400 으로 막으려면 `AccountCreateRequest` 의 칸에 `@NotNull` · `@NotBlank` 를 붙이고 컨트롤러에 `@Valid` 를 붙여야 합니다 (아래 ✍️ 1번).
- 실습 뒤에는 `{"customerId":1,"accountType":"적금"}` 으로 PUT, `{"status":"정상"}` 으로 PATCH 해서 되돌립니다.

## 🏦 은행에서는

계좌 정보는 대부분 **PATCH** 로 고칩니다(상태 변경, 한도 변경 등). 전체 교체(PUT)는 실수 한 번에 다른 칸을 지울 수 있어서, 쓰더라도 모든 칸을 검증합니다.

## 🧩 체크포인트

**결과를 먼저 예상해서 적고**, 그다음 보내서 확인합니다. 계좌는 `1002-345-112233` 입니다.

1. 아래 요청을 보내면 상태코드와 결과가 어떻게 될까요?
   - a) `PUT /api/accounts/1002-345-112233` + `{"accountType":"적금"}` (`customerId` 가 빠짐)
   - b) `PATCH /api/accounts/1002-345-112233/status` + `{}`
   - c) a) 와 같은 PUT 을 **두 번** 연달아 보내기
2. a) 의 응답에는 `customerId` 칸이 없습니다. 바뀐 것을 어디서 확인할 수 있을까요? (힌트: `GET /api/accounts/balance?minBalance=1000000&maxBalance=1100000`)

<details>
<summary>정답 보기</summary>

- a) **200**. 빠진 `customerId` 가 null 로 덮입니다 (PUT = 통째로 교체). 2번의 `balance` 조회에서 `"customerId":null` 이 보입니다.
- b) **200**, 아무 칸도 안 바뀝니다 (PATCH 는 보낸 칸만 수정하고, 보낸 칸이 없으니까요).
- c) 두 번 다 같은 결과입니다 (멱등).
- 되돌리기: `PUT` + `{"customerId":1,"accountType":"적금"}`

</details>

---

# 2. 계좌 해지: DELETE, 그런데 지우지 않는다

## 💡 규칙

금융 데이터는 **지우지 않고 상태를 바꿉니다.** 이것을 **Soft Delete** 라고 부릅니다. (행을 진짜 지우는 것은 Hard Delete)

## ❌ 우리 코드에서 찾기

지금 `DELETE` 는 행을 **진짜 지웁니다.**

```java
// AccountController
@DeleteMapping ("/{accountNo}")
public void deleteOne(@PathVariable("accountNo") String accountNo) {
    accountService.deleteOneByAccountNo(accountNo);    // → accountRepository.deleteByAccountNo() : DELETE FROM account ...
}
```

실제로 보내 보면 (H2 시드 기준):

```http
### ① 방금 개설한 새 계좌 (거래 없음)
DELETE http://localhost:8080/api/accounts/{새 계좌번호}

### ② 거래가 있는 시드 계좌
DELETE http://localhost:8080/api/accounts/1002-345-112233
```

```
① 200 → 다시 조회하면 404 ACCOUNT_NOT_FOUND   (행이 사라졌다. 기록도 함께 사라짐)
② 500 {"code":"INTERNAL_ERROR","message":"잠시 후 다시 시도해 주세요."}
   콘솔: Referential integrity constraint violation:
         "FK...: PUBLIC.TRANSACTION FOREIGN KEY(ACCOUNT_ID) REFERENCES PUBLIC.ACCOUNT(ACCOUNT_ID) (2)"
```

- ② 는 `transaction` 표가 이 계좌를 **외래키(FK)** 로 가리키고 있어서 지울 수 없습니다. **FK 가 계좌를 지켜 준 것**입니다. (MySQL 이면 `Cannot delete or update a parent row: a foreign key constraint fails` 로 같은 뜻)
- 하지만 ① 처럼 거래가 없는 계좌는 흔적 없이 사라집니다. 은행에서는 이것도 안 됩니다.

## ✅ 고치기 (실습: 지금 코드에는 없음)

`Account.close()` 는 이미 엔티티에 있습니다. 잔액이 0 이 아니면 `BalanceRemainingException` 을 던지고, 0 이면 `status = "해지"` 로 바꿉니다.

```java
// Account.java (이미 있음)
public void close() {
    if (balance.compareTo(BigDecimal.ZERO) != 0) {   // 0 과 0.00 은 equals 로는 다르다 → compareTo (5절)
        throw new BalanceRemainingException(" 계좌에 잔액 " + balance + "원이 남아 있어 해지할 수 없습니다.");
    }
    this.status = "해지";
}
```

세 군데를 고칩니다.

```java
// ① AccountService.java
@Transactional
public void close(String accountNo) {
    Account account = getAccount(accountNo);
    account.close();          // 잔액이 0 이 아니면 예외 → 409 BALANCE_REMAINING
    log.info("계좌 해지 accountNo={}", accountNo);
}                             // save() 없이도 변경 감지로 UPDATE 된다 (6절)

// ② AccountController.java: 기존 deleteOne 대신
@DeleteMapping("/{accountNo}")
public ResponseEntity<Void> close(@PathVariable("accountNo") String accountNo) {
    accountService.close(accountNo);
    return ResponseEntity.noContent().build();   // 204 No Content
}

// ③ GlobalExceptionHandler.java: 이게 없으면 409 가 아니라 500 이 된다
@ExceptionHandler(BalanceRemainingException.class)
public ResponseEntity<ErrorResponse> handleBalanceRemaining(BalanceRemainingException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("BALANCE_REMAINING", e.getMessage()));
}
```

고친 뒤 같은 요청을 보내면:

```
① 새 계좌 (잔액 0.00) → 204, 다시 조회하면 {"accountNo":"...","balance":0.00,"status":"해지"}   ← 행은 남고 상태만 바뀐다
   같은 DELETE 를 한 번 더 → 또 204 (이미 해지 상태 → 결과가 같다 = 멱등)
② 1002-345-112233 (잔액 1,099,500) → 409 {"code":"BALANCE_REMAINING","message":" 계좌에 잔액 1099500.00원이 남아 있어 해지할 수 없습니다."}
```

## 🏦 은행에서는

계좌를 해지해도 거래 기록은 **법적으로 수년간 보관**해야 합니다. 그래서 `DELETE` 요청을 받아도 행을 지우지 않고 `status = '해지'` 로 바꿉니다. 그리고 **잔액이 남은 계좌는 해지할 수 없습니다.** 손님 돈이 갈 곳을 잃으니까요.

## 🧩 체크포인트

2절의 Soft Delete 로 **고친 뒤**를 가정합니다. 해지(`status = 해지`)된 계좌에 아래를 보내면 어떻게 될까요?

```http
PATCH http://localhost:8080/api/accounts/{해지한 계좌번호}/status
Content-Type: application/json

{"status":"정상"}
```

<details>
<summary>정답 보기</summary>

**200 으로 되살아납니다.** `patchAccount` 가 지금 상태를 보지 않고, 보낸 값이 정상 · 휴면 · 지급정지 중 하나이면 그대로 바꾸기 때문입니다. 해지는 되돌릴 수 없어야 하는데 구멍이 있는 셈입니다.

(시간이 모자라면 건너뛰기) 막는 방법은 `patchAccount` 맨 앞에 한 줄을 넣는 것입니다. 넣어 보고 확인한 뒤 되돌려도 되고, 설계만 읽고 넘어가도 됩니다.

```java
if ("해지".equals(account.getStatus())) throw new InvalidAccountRequestException("해지된 계좌는 바꿀 수 없습니다.");
```

</details>

---

# 3. 페이징: 거래내역을 한 쪽씩

## 🤔 왜 필요할까요?

거래가 3만 건인 계좌의 내역을 한 번에 보내면 서버도, 네트워크도, 화면도 힘듭니다. 그래서 **`page` 와 `size`** 로 "몇 번째 쪽을 몇 건씩" 달라고 요청합니다.

> 💡 **책의 쪽수** : 3만 줄짜리 책을 통째로 주지 않고 "20줄씩, 0쪽부터" 로 나눠 줍니다. 총 몇 쪽인지(`totalPages`)도 함께 알려 줍니다.

## 💻 우리 코드

```java
// AccountController
// GET /api/accounts/1002-345-678901/transactions?page=0&size=20
@GetMapping("/{accountNo}/transactions")
public Page<TransactionResponse> transactions(@PathVariable("accountNo") String accountNo, Pageable pageable) {
    // 정렬은 서버가 최신순으로 고정한다
    Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "txDatetime"));
    return accountService.getTransactions(accountNo, newestFirst).map(TransactionResponse::from);
}
```

```java
// TransactionRepository: 이름만 쓰면 JPA 가 쿼리를 만들어 준다. 마지막 인자가 Pageable
Page<Transaction> findByAccount_AccountNo(String accountNo, Pageable pageable);
```

- `?page=0&size=20` 을 **스프링이 `Pageable` 로 바꿔** 넘겨 줍니다. 직접 파싱하지 않습니다.
- **`page` 는 0부터** 시작합니다. 첫 쪽이 0, 두 번째 쪽이 1 입니다.
- 정렬은 **서버가 최신순(`txDatetime` 내림차순)으로 고정**합니다. 손님이 `?sort=` 를 보내도 `PageRequest.of(...)` 로 새로 만들 때 버려집니다.
- 반환 타입이 `Page` 라서 JPA 는 쿼리를 **두 번** 보냅니다. 한 쪽 분량만 가져오는 **목록 쿼리**와 전체 건수를 세는 **`count(*)` 쿼리**입니다. `totalElements` · `totalPages` 는 이 두 번째 쿼리 덕분에 알 수 있습니다.

## 🪵 실행 · 확인

시드 계좌 1번(`1002-345-678901`)에는 거래가 3건 있습니다. 2건씩 받아 봅니다.

```http
### 첫 쪽
GET http://localhost:8080/api/accounts/1002-345-678901/transactions?page=0&size=2

### 둘째 쪽
GET http://localhost:8080/api/accounts/1002-345-678901/transactions?page=1&size=2
```

`page=0&size=2` 실제 응답 (줄여서 적었습니다) :

```json
{
  "content": [
    { "txId": 3, "txType": "출금", "amount": 12000.0, "memo": "점심", "counterparty": "김밥천국", "txDatetime": "2026-09-02T12:31:00", ... },
    { "txId": 2, "txType": "입금", "amount": 2400000.0, "memo": "9월급여", ... }
  ],
  "empty": false,
  "first": true,
  "last": false,
  "number": 0,
  "numberOfElements": 2,
  "pageable": { "offset": 0, "pageNumber": 0, "pageSize": 2, ... },
  "size": 2,
  "sort": { ... },
  "totalElements": 3,
  "totalPages": 2
}
```

`page=1&size=2` 는 `content` 에 **1건**(운동화)만 들어 있고 `"first": false`, `"last": true` 입니다.

| 필드 | 뜻 |
|---|---|
| `content` | 이번 쪽의 거래 목록 (최신순) |
| `number` | 지금 몇 번째 쪽인가 (0부터) |
| `size` | 한 쪽에 몇 건을 요청했나 |
| `numberOfElements` | 이번 쪽에 실제로 들어온 건수 (마지막 쪽은 `size` 보다 작을 수 있다) |
| `totalElements` · `totalPages` | 전체 건수 · 전체 쪽 수. 3건을 2건씩 나누면 `ceil(3 ÷ 2)` = **2쪽** |
| `first` · `last` | 첫 쪽인가 · 마지막 쪽인가. 화면의 "다음" 버튼을 막을 때 씁니다 |

**콘솔 확인** : 요청 한 번에 SQL 이 두 번 나갑니다. 목록 쿼리와 `count(*)` 쿼리입니다.

또 콘솔에 이런 경고가 한 줄 찍힙니다.

```
Serializing PageImpl instances as-is is not supported, meaning that there is no guarantee about the stability of the resulting JSON structure!
```

`Page` 객체를 그대로 JSON 으로 만드는 모양은 **라이브러리 버전에 따라 바뀔 수 있다**는 경고입니다. 위의 `pageable` · `sort` 같은 필드가 그 예입니다. Day 3(`t6-react`)에서 `WebConfig` 한 줄로 `{content, page}` 모양으로 고정합니다 ([day3-react.md](day3-react.md) 3절).

🚨 지금 `AccountController` 의 이 메서드 위 주석에는 응답이 `{ "content": [...], "page": {...} }` 모양이라고 적혀 있습니다. 그것은 **Day 3 에서 `WebConfig` 를 만든 뒤의 결과**를 적은 것이고, Day 0 에서 실제로 받는 응답은 위의 모양입니다.

### 경계값

| 요청 | 결과 |
|---|---|
| `size=5000` | **2000** 으로 잘린다 (스프링 기본 최대값) |
| `size=0` | **20** (기본값) |
| `page=-1` · `page=abc` | **0쪽** 으로 처리된다 |
| 없는 계좌 | 404 `ACCOUNT_NOT_FOUND` |

잘못된 값이 와도 에러가 아니라 **기본값으로 보정**됩니다. 큰 `size` 로 서버를 괴롭히는 요청도 상한이 막아 줍니다.

## 🏦 은행에서는

거래내역은 "최근 3개월" 처럼 기간 조건과 함께 쪽 단위로 줍니다. 한 번에 보내는 건수에 **상한**을 두는 것이 기본입니다.

## ✍️ 바로 해보기

1. `size=1` 로 `page=0`, `1`, `2`, `3` 을 차례로 불러 보세요. 몇 쪽부터 `content` 가 비어 있고, `last` 는 어느 쪽에서 `true` 가 되나요?
2. 거래가 7건이고 `size=3` 이면 `totalPages` 는 몇이고, 마지막 쪽의 `numberOfElements` 는 몇일까요?

<details>
<summary>정답 보기</summary>

1. `page=0`, `1`, `2` 는 각각 1건, `last` 는 `page=2` 에서 `true` 입니다. `page=3` 은 에러가 아니라 **빈 `content`** 로 옵니다 (`totalPages` 는 3).
2. `ceil(7 ÷ 3)` = **3쪽**이고, 마지막 쪽(`page=2`)에는 7 − 3 − 3 = **1건**이 들어 있습니다.

</details>

---

# 4. Header 와 Body: 봉투의 겉면과 속지

## 💡 개념

| 구분 | 비유 | 담는 것 | 예 |
|---|---|---|---|
| **Header** | 봉투 **겉면** | 요청에 대한 **정보** | `Content-Type`, `Authorization`, `Idempotency-Key`, `X-Request-Id` |
| **Body** | 봉투 속 **편지** | 요청의 **내용** | `{"customerId":1, "accountType":"입출금"}` |

- 서버는 겉면의 `Content-Type: application/json` 을 보고 "속지는 JSON 이구나" 하고 `@RequestBody` 로 읽습니다.
- 응답도 같은 모양입니다. 응답 헤더 `Content-Type: application/json` + 응답 본문 `{...}`.

## 💻 우리 코드

```java
// AccountController: Body 는 @RequestBody 로 받는다. Header 를 읽는 코드는 아직 없다
@PostMapping
public AccountResponse createAccount(@RequestBody AccountCreateRequest request) {
    Account account = accountService.createAccount(request);
    return AccountResponse.from(account);          // 200 + 계좌 정보
}
```

- 지금 코드는 **200** 과 본문만 돌려줍니다. 바로 위에 주석으로 남아 있는 `ResponseEntity.created(location)` 버전을 쓰면 **201 Created** 와 함께 응답 헤더 **`Location: /api/accounts/{새 계좌번호}`** 가 붙습니다 ("새로 만든 것은 여기 있어요" 라는 겉면 정보).

## 🪵 실행 · 확인

```http
### 정상: Header 에 Content-Type, Body 에 JSON
POST http://localhost:8080/api/accounts
Content-Type: application/json

{"customerId":1,"accountType":"적금"}
```

```
HTTP/1.1 200
Content-Type: application/json
{"accountNo":"1306b3d5f4544cedb84b","accountType":"적금","balance":0.00,"status":"정상"}
```

`Content-Type` 줄을 지우거나 `text/plain` 으로 바꿔 보내면:

```
HTTP/1.1 415
{"code":"UNSUPPORTED_MEDIA_TYPE","message":"Content-Type 'text/plain;charset=UTF-8' is not supported."}
```

**415 Unsupported Media Type.** 서버가 "속지가 JSON 인지 모르겠어요" 하고 거절합니다. 헤더는 장식이 아닙니다.

## ✅ 헤더 직접 읽어 보기 (실습: 지금 코드에는 없음)

```java
@PostMapping
public AccountResponse createAccount(
        @RequestHeader(value = "X-Request-Id", required = false) String requestId,   // 겉면: 없어도 된다(required = false)
        @RequestBody AccountCreateRequest request) {                               // 속지
    log.info("계좌 개설 요청 requestId={}", requestId);
    ...
}
```

- `required = false` 를 빼면 그 헤더 없이 보낸 요청은 **400** 이 됩니다.
- Day 1 의 `Idempotency-Key`, Day 2 의 `Authorization: Bearer 토큰` 도 모두 이 **겉면**에 실립니다.

## ✍️ 바로 해보기

1. Postman, Boomerang, Swagger **세 곳**에서 같은 POST 를 보내 보고, 각 도구에서 요청 헤더 · 응답 헤더가 어디에 보이는지 찾아보세요
2. `Content-Type` 을 빼고 보내 보세요. 몇 번이 오나요?
3. 응답 헤더에서 `Location` 을 찾아보세요. 지금은 **없습니다.** `createAccount` 를 주석 처리된 `ResponseEntity.created(...)` 버전으로 바꾸면(`URI` · `ServletUriComponentsBuilder` · `ResponseEntity` import 필요, 기존 `createAccount` 는 주석 처리) 201 과 `Location` 이 생기는지 확인하세요

<details>
<summary>정답 보기</summary>

2. **415 Unsupported Media Type** (`UNSUPPORTED_MEDIA_TYPE`).
3. `HTTP/1.1 201` 과 `Location: http://localhost:8080/api/accounts/{새 계좌번호}` 가 생깁니다. 본문은 그대로입니다.

</details>

---

# 5. BigDecimal: 돈은 double 로 계산하지 않는다

## 🤔 왜 필요할까요?

`double` 은 0.1 을 정확히 저장하지 못합니다. 1원 오차도 은행에서는 사고입니다.

```
jshell> 0.1 + 0.2
$1 ==> 0.30000000000000004

jshell> new java.math.BigDecimal("0.1").add(new java.math.BigDecimal("0.2"))
$2 ==> 0.3
```

## 💻 우리 코드

```java
// Account.java
@Column(name = "balance", precision = 19, scale = 2)   // DB 의 DECIMAL(19,2): 전체 19자리, 소수 2자리
private BigDecimal balance;

account.balance = new BigDecimal("0.00");                // 개설: 문자열로 만든다

public void withdraw(BigDecimal amount) {
    if (balance.compareTo(amount) < 0) { ... 잔액 부족 ... }   // balance < amount 를 이렇게 쓴다
    balance = balance.subtract(amount);                        // 결과를 다시 담는다
}
```

## 규칙 네 가지

| 규칙 | 틀린 코드 | 맞는 코드 | 이유 |
|---|---|---|---|
| 만들 때는 **문자열**로 | `new BigDecimal(0.1)` | `new BigDecimal("0.1")` | `new BigDecimal(0.1)` 은 이미 틀어진 double 값을 받아 `0.1000000000000000055511...` 이 된다 |
| 계산은 **메서드**로 | `balance - amount` | `balance.subtract(amount)` | 객체라서 `+ - < >` 연산자가 없다 |
| 결과를 **다시 대입** | `balance.subtract(amount);` | `balance = balance.subtract(amount);` | 불변 객체: 원본은 안 바뀌고 새 객체가 돌아온다. 대입을 빼면 잔액이 그대로다 |
| 비교는 **compareTo** | `balance.equals(BigDecimal.ZERO)` | `balance.compareTo(BigDecimal.ZERO) == 0` | `0` 과 `0.00` 은 `equals` 로는 **다르다** (소수 자릿수까지 비교). `Account.close()` 가 compareTo 를 쓰는 이유 |

```
jshell> new BigDecimal("0").equals(new BigDecimal("0.00"))
$3 ==> false
jshell> new BigDecimal("0").compareTo(new BigDecimal("0.00"))
$4 ==> 0
```

- 응답의 `"balance":1099500.00` 처럼 소수 둘째 자리까지 나오는 것은 DB 칸이 `DECIMAL(19,2)` 이기 때문입니다.

## 🏦 은행에서는

금액은 `BigDecimal` + DB `DECIMAL` 이 기본입니다. 이자처럼 나누기가 있으면 `divide(x, 2, RoundingMode.HALF_UP)` 처럼 **자릿수와 반올림 방식을 꼭 적습니다** (안 적으면 0.333... 처럼 끝나지 않는 나눗셈에서 예외가 납니다).

## 🧩 체크포인트

**결과를 먼저 종이에 적고**, jshell 로 확인합니다. jshell 에서 `import java.math.*;` 를 먼저 실행하세요.

| | 식 | 내 예상 |
|---|---|---|
| A | `new BigDecimal("10").divide(new BigDecimal("4"))` | |
| B | `new BigDecimal("10").divide(new BigDecimal("3"))` | |
| C | `new BigDecimal("10").divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP)` | |
| D | `new BigDecimal("1.005").setScale(2, RoundingMode.HALF_UP)` | |
| E | `new BigDecimal(1.005).setScale(2, RoundingMode.HALF_UP)` | |
| F | `new BigDecimal("2.50").equals(new BigDecimal("2.5"))` 와 `compareTo` | |
| G | `BigDecimal b = new BigDecimal("100"); b.add(new BigDecimal("50"));` 다음 `b` 는? | |

<details>
<summary>정답 보기</summary>

| | 결과 | 이유 |
|---|---|---|
| A | `2.5` | 딱 떨어지는 나눗셈은 된다 |
| B | `ArithmeticException: Non-terminating decimal expansion; no exact representable decimal result.` | 3.333… 은 끝나지 않아서, 자릿수를 안 적으면 예외 |
| C | `3.33` | 자릿수(2)와 반올림 방식을 적었다 |
| D | `1.01` | 문자열 `"1.005"` 는 정확히 1.005 |
| E | **`1.00`** | `double` 1.005 는 사실 1.00499999… 라서 반올림이 내려간다. 문자열로 만들어야 하는 이유 |
| F | `equals` → `false`, `compareTo` → `0` | 소수 자릿수(2.50 / 2.5)까지 비교하는 것이 `equals` |
| G | `100` | 불변이라 원본은 그대로. `b = b.add(...)` 처럼 **다시 대입**해야 한다 |

</details>

---

# 6. @Transactional: 한 묶음으로, 그리고 변경 감지

## 🤔 왜 필요할까요?

"출금은 됐는데 입금은 안 된" 상태를 막아야 합니다. 여러 DB 작업을 **전부 성공 아니면 전부 취소(롤백)** 로 묶는 것이 트랜잭션입니다.

## 💻 우리 코드

```java
// AccountService
@Transactional
public Account updateOne(String accountNo, AccountCreateRequest request) {   // PUT
    Account account = lockAccount(accountNo);
    account.setCustomerId(request.customerId());
    account.setAccountType(request.accountType());
    return account;                       // save() 를 부르지 않는다
}

@Transactional
public Account patchAccount(String accountNo, AccountPatchRequest request) {   // PATCH
    Account account = lockAccount(accountNo);
    if (request.accountType() != null) { ... account.setAccountType(...); }      // ① 먼저 바꾸고
    if (request.status() != null) { ... else throw new InvalidAccountRequestException(...); }   // ② 여기서 터지면?
    return accountRepository.save(account);
}
```

`@Transactional` 이 해 주는 일은 두 가지입니다.

| | 하는 일 | 우리 코드에서 |
|---|---|---|
| **한 묶음 (원자성)** | 메서드가 끝나면 커밋, 도중에 `RuntimeException` 이 나면 **전부 롤백** | `patchAccount` 에서 ① 뒤에 ② 가 터지면 ① 도 취소 |
| **변경 감지 (dirty checking)** | 트랜잭션 안에서 조회한 엔티티의 값을 바꾸면, 끝날 때 JPA 가 바뀐 칸을 찾아 `UPDATE` 를 보낸다 | `updateOne` 은 `save()` 없이도 DB 가 바뀐다 |

## 🪵 실행 · 확인

**① 변경 감지**: 1절의 PUT ① 을 보내고 콘솔을 봅니다. `save()` 를 부르지 않았는데 `UPDATE` 가 나갑니다.

```
select ... from account a1_0 where a1_0.account_no=? for update
update account set account_no=?, account_type=?, balance=?, customer_id=?, status=? where account_id=?
```

**② 롤백**: `accountType` 은 맞게, `status` 는 틀리게 보냅니다.

```http
PATCH http://localhost:8080/api/accounts/1002-345-112233/status
Content-Type: application/json

{"accountType":"입출금","status":"이상"}
```

```
→ 400 {"code":"INVALID_ACCOUNT_REQUEST","message":"상태는 정상 · 휴면 · 지급정지 중 하나여야 합니다."}
→ 다시 조회하면 accountType 은 "적금" 그대로   (① 에서 바꾼 accountType 까지 롤백됐다)
```

## 🚨 자주 하는 실수

| 실수 | 결과 |
|---|---|
| `lockAccount()` 를 쓰는 메서드에 `@Transactional` 을 빼먹기 | **500** (`TransactionRequiredException: No active transaction`). 잠금 조회는 트랜잭션 안에서만 된다 (7절) |
| 같은 클래스 안에서 `this.다른메서드()` 로 `@Transactional` 메서드를 부르기 | 트랜잭션이 안 걸린다. `@Transactional` 은 스프링이 만든 **대리 객체(프록시)** 를 거칠 때만 동작하는데, `this` 호출은 프록시를 건너뛴다 |
| `try-catch` 로 예외를 삼키기 | 예외가 메서드 밖으로 안 나가서 **롤백되지 않는다** |
| `Exception`(checked) 을 던지기 | 기본 설정은 `RuntimeException` 만 롤백한다. 우리 예외들이 `RuntimeException` 을 상속하는 이유 |

- 💬 `AccountController` · `patchAccount` 근처의 주석 "변경 감지가 없으므로 save() 를 직접 부른다" 는 `@Transactional` 을 붙이기 **전** 설명입니다. 지금은 변경 감지가 되므로 `patchAccount` 의 `save()` 는 없어도 결과가 같습니다 (✍️ 2번).

## 🏦 은행에서는

이체는 "A 출금 + B 입금 + 거래내역 2건" 이 한 트랜잭션입니다. Day 3 에서 만드는 `transfer()` 에서 휴면 계좌로 이체하면 출금까지 롤백되는 것을 확인합니다.

## 🧩 체크포인트

**한 줄 고장 내기**: 예외를 삼키면 롤백이 될까요? 먼저 예상한 뒤 해 봅니다.

1. `AccountService.patchAccount` 에서 `if (request.status() != null) { ... }` 블록 **전체**를 아래처럼 감싸고 앱을 다시 시작합니다.

```java
try {
    if (request.status() != null) { ... }
} catch (InvalidAccountRequestException e) {
    log.warn("삼켰다: {}", e.getMessage());
}
```

2. 6절 ② 와 같은 요청을 보냅니다.

```http
PATCH http://localhost:8080/api/accounts/1002-345-112233/status
Content-Type: application/json

{"accountType":"입출금","status":"이상"}
```

3. 상태코드는? 그리고 `accountType` 은 "적금" 그대로일까요, "입출금" 으로 바뀔까요?

<details>
<summary>정답 보기</summary>

**200** 이고, `accountType` 은 **"입출금" 으로 저장됩니다 (롤백 안 됨).** 예외를 `catch` 로 삼키면 예외가 메서드 밖으로 나가지 않아서 `@Transactional` 이 실패를 모릅니다. 그래서 정상 종료로 보고 커밋합니다. 6절 "자주 하는 실수" 표의 `try-catch` 줄이 바로 이 모습입니다.

확인한 뒤에는 **코드를 원래대로 되돌리고**, 바뀐 계좌는 `PUT` + `{"customerId":1,"accountType":"적금"}` 로 복구합니다.

</details>

---

# 7. 비관적 락: 동시에 같은 계좌를 고치면?

## 🤔 왜 필요할까요?

잔액 100만 원 계좌에서 두 요청이 **동시에** 1만 원씩 출금한다고 해 봅니다.

| 시간 | 요청 A | 요청 B | DB 잔액 |
|---|---|---|---|
| 1 | 잔액 읽음: 1,000,000 | | 1,000,000 |
| 2 | | 잔액 읽음: 1,000,000 | 1,000,000 |
| 3 | 990,000 으로 저장 | | 990,000 |
| 4 | | 990,000 으로 저장 | **990,000** ← 2만 원이 빠져야 하는데 1만 원만 빠졌다 |

B 가 A 의 결과를 덮어써서 출금 한 건이 **사라졌습니다** (Lost Update). 스레드가 무엇이고 왜 동시에 실행되는지는 [reference/threads.md](reference/threads.md) 에 있습니다.

## 💻 우리 코드

```java
// AccountRepository: 잠금 조회. JPA 가 SELECT ... FOR UPDATE 를 만들어 준다
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select a from Account a where a.accountNo = :accountNo")
Optional<Account> findByAccountNoForUpdate(@Param("accountNo") String accountNo);

// AccountService: 고치기 전에는 getAccount() 대신 lockAccount() 로 읽는다
private Account lockAccount(String accountNo) {
    return accountRepository.findByAccountNoForUpdate(accountNo)
            .orElseThrow(() -> new AccountNotFoundException(accountNo + " 계좌를 찾을 수 없습니다."));
}
```

- `FOR UPDATE` 로 읽은 행은 **트랜잭션이 끝날 때까지** 다른 트랜잭션이 잠금 조회 · 수정을 못 하고 **기다립니다**. 위 표에서 B 는 2번 시간에 A 가 끝날 때까지 기다렸다가 990,000 을 읽고 980,000 으로 저장합니다.
- 잠금은 "트랜잭션이 끝날 때 풀린다" 가 규칙이라, **트랜잭션이 없으면 잠글 수 없습니다.** 그래서 `lockAccount()` 를 쓰는 메서드에는 `@Transactional` 이 꼭 있어야 합니다 (6절의 500 에러).
- "비관적" 인 이유: 충돌이 **날 거라고 보고** 미리 잠그기 때문입니다.

## 🪵 실행 · 확인

**① SQL 로그**: 1절의 PUT 이나 PATCH 를 보내면 콘솔의 SELECT 끝에 `for update` 가 붙어 있습니다.

```
select a1_0.account_id, ... from account a1_0 where a1_0.account_no=? for update
```

**② H2 콘솔 두 탭으로 기다림 보기** (`http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:mem:testdb`, 사용자 `sa`, 비밀번호 없음)

탭 두 개를 열고, 두 탭 모두 위쪽의 **Auto commit 체크를 끕니다.**

| 순서 | 탭 1 | 탭 2 |
|---|---|---|
| 1 | `SELECT * FROM account WHERE account_no='1002-345-112233' FOR UPDATE;` | |
| 2 | | `SET LOCK_TIMEOUT 10000;` 실행 후 같은 `SELECT ... FOR UPDATE;` → **멈춰서 기다린다** |
| 3 | `UPDATE account SET balance = balance - 10000 WHERE account_no='1002-345-112233';` → `COMMIT;` | |
| 4 | | 그제서야 결과가 나오고, 잔액은 **1만 원 줄어든 값** |

- 탭 2 에서 `SET LOCK_TIMEOUT` 을 안 하면 H2 기본값(약 2초) 뒤에 `Timeout trying to lock table "ACCOUNT"` 로 실패합니다. 이것도 "잠겨 있어서 못 들어갔다" 는 증거입니다.
- 끝나면 탭 2 도 `COMMIT;` 합니다. 앱을 다시 켜면 시드 데이터로 돌아옵니다.

## 비관적 락 vs 낙관적 락

| | 비관적 락 (우리 코드) | 낙관적 락 |
|---|---|---|
| 방법 | 읽을 때 `FOR UPDATE` 로 잠근다 | 엔티티에 `@Version` 칸을 두고, 저장할 때 버전이 바뀌었으면 실패시킨다 |
| 충돌하면 | 뒤 요청이 **기다린다** | 뒤 요청이 **실패한다** (다시 시도해야 함) |
| 어울리는 곳 | 충돌이 잦고 실패하면 안 되는 곳: **잔액** | 충돌이 드문 곳: 게시글 수정 등 |

## 🚨 주의

- **잠그는 순서**: 이체처럼 계좌 두 개를 잠글 때 A→B, B→A 순서가 섞이면 서로를 기다리는 **데드락**이 납니다. Day 3 의 `transfer()` 는 항상 **계좌번호 순서**로 잠급니다.
- **읽기만 할 때는 잠그지 않는다**: 잠금은 다른 요청을 기다리게 만듭니다. 지금 코드의 `getTransactions()` 는 거래내역을 **읽기만** 하는데도 `lockAccount()` 를 씁니다. 계좌가 있는지 확인만 하면 되므로 `getAccount()` 로 충분합니다 (✍️ 3번).

---

# 8. JUnit 으로 테스트 코드 작성 및 검증

## 🤔 클릭 한 번으로 확인하는 것의 한계

Swagger 에서 버튼을 눌러 보면 코드가 잘 동작합니다. "잘 되네요!" 그런데 정말일까요?

| 사람이 클릭으로 확인할 때 | 실제 서비스에서는 |
|---|---|
| 요청은 **한 번에 하나**씩, 버튼은 **한 번만** 누릅니다 | 손님은 연타하고, 앱은 타임아웃이 나면 **자동으로 다시** 보냅니다 |
| 오늘 만든 기능만 확인합니다 | 내일 코드를 고치면 **어제 만든 기능이 깨졌는지** 다시 눌러 보기 어렵습니다 |
| "안 되어야 하는 경우"(지급정지 · 잔액 부족)는 귀찮아서 건너뜁니다 | 돈 문제는 **막혀야 할 때 막히는지**가 더 중요합니다 |

> **눈으로 확인한 것은 "한 번 됐다" 이지 "항상 된다" 가 아닙니다.** 그래서 확인을 코드로 적어 두고, 명령 한 번으로 다시 돌립니다.

## 💡 JUnit

자바의 대표 테스트 프레임워크입니다. **어노테이션(`@Test`)** 을 붙인 메서드가 테스트가 되고, **단정문(assert)** 으로 기댓값이 나왔는지 확인합니다. 우리 프로젝트는 **JUnit 6** (Spring Boot 4.1 기본, Java 17 이상)을 씁니다.

| 모듈 | 하는 일 |
|---|---|
| **JUnit Platform** | 테스트를 찾고 실행하고 결과를 보고하는 기반. Gradle · IntelliJ 가 이걸 통해 테스트를 돌린다 |
| **JUnit Jupiter** | 우리가 쓰는 쪽: `@Test` · `@BeforeEach` 같은 어노테이션과 assert API, 그리고 이를 실행하는 엔진 |
| **JUnit Vintage** | 옛 JUnit 3 · 4 테스트를 돌리는 엔진. JUnit 6 에서는 deprecated (우리는 안 씀) |

우리 `build.gradle` 의 테스트 의존성 (Spring Boot 4 는 테스트 스타터가 **기술별로 나뉘어** 있습니다):

```groovy
testImplementation 'org.springframework.boot:spring-boot-starter-data-jpa-test'   // @DataJpaTest + (따라오는 spring-boot-starter-test 로) JUnit · AssertJ · Mockito
testImplementation("org.springframework.boot:spring-boot-webmvc-test:4.1.1")       // @WebMvcTest · MockMvc  ← 빼면 컴파일부터 실패
testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
```

## 💡 테스트 피라미드: 작은 테스트는 많이, 큰 테스트는 적게

부품 검사(싸고 빠름)는 수천 번, 완성차 시험 주행(비싸고 느림)은 몇 번만 하는 자동차 공장과 같습니다. 우리 레포에 이미 있는 테스트로 보면:

| 종류 | 대상 | 우리 테스트 파일 | 어노테이션 | 속도 |
|---|---|---|---|---|
| 단위 (도메인) | `Account.withdraw()` | `model/AccountTest` | 없음 (그냥 `@Test`) | 매우 빠름 |
| 단위 (서비스) | `AccountService` | `service/AccountServiceTest` | `@ExtendWith(MockitoExtension.class)` | 빠름 |
| 슬라이스 (저장소) | `AccountRepository` | `repository/AccountRepositoryTest` | `@DataJpaTest` | 보통 |
| 슬라이스 (웹) | `AccountController` | `controller/AccountControllerTest` | `@WebMvcTest` + MockMvc | 보통 |
| 통합 | 앱 전체 | `WonitApplicationTests` | `@SpringBootTest` | 느림 |

> 💡 **슬라이스 테스트**: 앱 전체를 띄우지 않고 **한 층(저장소 층, 웹 층)만 잘라서** 띄우는 테스트입니다. 건물 전체 점검 대신 "배관만", "전기만" 따로 점검하는 것과 같습니다.

## 💡 기본 모양: given · when · then 과 AssertJ

테스트를 **준비 → 실행 → 확인** 세 덩어리로 씁니다. "이런 계좌가 있을 때(given), 이렇게 하면(when), 이래야 한다(then)". `src/test/java/com/example/wonit/MyTest.java` 가 가장 작은 예입니다.

```java
@Test
public void givenInitialState_whenActionIsPerformed_thenExpectedOutcome() {
    int initialValue = 1;                              // Given: 초기 상태
    int result = addOne(initialValue);                 // When: 동작 수행
    assertEquals(2, result, "1 + 1 should equal 2");   // Then: 결과 확인
}
```

| 하고 싶은 말 | AssertJ 문장 |
|---|---|
| 값이 같다 | `assertThat(a).isEqualTo(b)` |
| **금액**이 같다 (🚨 BigDecimal) | `assertThat(balance).isEqualByComparingTo("90000")` |
| 예외가 난다 | `assertThatThrownBy(() -> ...).isInstanceOf(XXX.class)` |
| 예외가 안 난다 | `assertThatCode(() -> ...).doesNotThrowAnyException()` |

> 🚨 **BigDecimal 은 `isEqualTo` 로 비교하지 마세요.** `1.0` 과 `1.00` 은 `equals` 가 `false` 입니다 (5절의 함정). 금액은 `isEqualByComparingTo`(= `compareTo` 가 0)로 비교합니다.

## 💡 JUnit 의 생명주기: `TestLifeCycle.java`

| 어노테이션 | 언제 실행되나 |
|---|---|
| `@BeforeAll` | 이 클래스의 테스트를 **모두 시작하기 전 한 번** (`static`) |
| `@BeforeEach` | **각** 테스트 메서드 **전마다** |
| `@Test` | 테스트 메서드 |
| `@AfterEach` | **각** 테스트 메서드 **뒤마다** |
| `@AfterAll` | 이 클래스의 테스트가 **모두 끝난 뒤 한 번** (`static`) |
| `@Disabled` | 실행하지 않는다 (건너뜀으로 표시) |
| `@DisplayName("...")` | 결과 화면에 메서드 이름 대신 보여 줄 이름 |

`TestLifeCycle` 을 실행하면 콘솔에 이 순서로 찍힙니다 (test1 · test2 의 순서는 바뀔 수 있습니다).

```
## BeforeAll Annotation 호출 ##
## BeforeEach Annotation 호출 ##
## test1 시작 ##
## AfterEach Annotation 호출 ##
## BeforeEach Annotation 호출 ##
## test2 시작 ##
## AfterEach Annotation 호출 ##
## AfterAll Annotation 호출 ##
```

- `test3` 은 `@Disabled` 라서 실행되지 않고, 그 앞뒤의 `@BeforeEach` · `@AfterEach` 도 돌지 않습니다. 결과에는 **skipped(건너뜀) 1개**로 표시됩니다.

## 🧪 ① 도메인 단위 테스트: Spring 없이 (`model/AccountTest`)

`Account.withdraw()` 는 그냥 자바 메서드라서 **Spring 도, DB 도 필요 없습니다.** `new`(여기서는 `Account.open`)로 만들어 호출하고 결과만 봅니다. 0.01초면 끝나서 규칙이 많은 곳(지급정지 · 잔액 부족)은 여기서 촘촘히 확인합니다.

```java
@Test
@DisplayName("정상 계좌에서 출금하면 잔액이 줄어든다")
void withdraw_reducesBalance() {
    Account account = Account.open(1, "1002-111-111111", "입출금");   // given
    account.deposit(new BigDecimal("100000"));
    account.withdraw(new BigDecimal("30000"));                        // when
    assertThat(account.getBalance()).isEqualByComparingTo("70000");   // then
}

@Test
@DisplayName("잔액보다 큰 금액은 출금할 수 없다")
void withdraw_insufficientBalance() {
    Account account = Account.open(1, "1002-111-111111", "입출금");
    account.deposit(new BigDecimal("5000"));

    assertThatThrownBy(() -> account.withdraw(new BigDecimal("50000")))
            .isInstanceOf(InsufficientBalanceException.class);
    assertThat(account.getBalance()).isEqualByComparingTo("5000");   // 🚨 막힌 출금이 잔액을 건드리지 않았는지도 확인
}

// 실습1: 경계값 — 잔액과 똑같은 금액은 출금된다
@Test
void withdraw_exactBalance() {
    Account account = Account.open(1, "1002-111-111111", "입출금");
    account.deposit(new BigDecimal("10000"));
    assertThatCode(() -> account.withdraw(new BigDecimal("10000"))).doesNotThrowAnyException();
    assertThat(account.getBalance()).isEqualByComparingTo("0");
}
```

## 🧪 ② 서비스 단위 테스트: 가짜 Repository 로 (`service/AccountServiceTest`, Mockito)

Service 의 **흐름만** 확인하고 싶은데 매번 진짜 DB 를 띄울 필요는 없습니다.

> 💡 **Mock(목)**: **연습용 가짜**입니다. 소방 훈련에서 진짜 불 대신 연기만 피우듯, 진짜 DB 대신 "이 계좌번호로 물어보면 이 계좌를 돌려줘" 라고 **대본을 정해 둔 가짜 Repository** 를 끼웁니다. 이 가짜를 만들어 주는 도구가 **Mockito** 입니다.

| 도구 | 하는 일 |
|---|---|
| `@Mock` | 가짜 객체를 만든다 |
| `@InjectMocks` | 진짜 `AccountService` 를 만들고 가짜들을 생성자로 끼워 넣는다 (DI 와 같은 원리) |
| `given(...).willReturn(...)` | 가짜의 **대본**: "이렇게 물어보면 이렇게 답해" |
| `verify(...)` | 가짜가 **호출됐는지** 확인 |

```java
@ExtendWith(MockitoExtension.class)   // Spring 을 띄우지 않고 Mockito 만 켠다
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @InjectMocks private AccountService accountService;

    @Test
    void getAccount_계좌번호로_정상조회() {
        String accountNo = "1002-111-111111";                               // given
        Account account = Account.open(1, accountNo, "입출금");
        given(accountRepository.findByAccountNo(accountNo)).willReturn(Optional.of(account));

        Account result = accountService.getAccount(accountNo);              // when

        assertThat(result.getAccountNo()).isEqualTo(accountNo);             // then
        assertThat(result.getAccountType()).isEqualTo("입출금");
        verify(accountRepository).findByAccountNo(accountNo);               // 가짜가 정말 불렸나
    }
}
```

## 🧪 ③ 저장소 슬라이스: `@DataJpaTest` (`repository/AccountRepositoryTest`)

```java
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // "spring.sql.init.mode=never"     // 켜면 data.sql(시드)을 테스트 DB 에 넣지 않는다
})
class AccountRepositoryTest {

    @Autowired AccountRepository accountRepository;

    @Test
    @DisplayName("계좌번호로 계좌를 찾는다")
    void findByAccountNo() {
        Account saved = accountRepository.save(Account.open(10, "110-11-111111", "입출금"));

        Optional<Account> found = accountRepository.findByAccountNo(saved.getAccountNo());

        assertThat(found).isPresent();
        assertThat(found.get().getBalance()).isEqualByComparingTo("0.00");
    }   // @DataJpaTest 는 테스트가 끝나면 자동으로 롤백 → DB 가 깨끗하게 돌아온다
}
```

- 테스트용 H2 에도 시드(`data.sql`)가 들어갑니다. 그래서 시드에 있는 `1002-345-678901` 로 저장하면 같은 번호가 **두 건**이 되어 `findByAccountNo` 가 `IncorrectResultSizeDataAccessException` 으로 실패합니다 (주석 처리된 줄이 그 실험). 이 테스트가 보는 것은 "쿼리가 만들어지고 값이 맞는가" 까지입니다.

## 🧪 ④ 웹 슬라이스: `@WebMvcTest` + MockMvc (`controller/AccountControllerTest`)

> 💡 **MockMvc**: 서버를 실제로 띄우지 않고 **"HTTP 요청이 왔다고 치고"** Controller 에 던져 보는 도구입니다.
> 💡 **`@MockitoBean`**: 스프링 Bean 자리에 Mockito 가짜를 넣습니다. 예전 자료의 `@MockBean` 은 Spring Boot 4 에서 **없어졌고**, 이름이 `@MockitoBean` 으로 바뀌었습니다.

```java
@WebMvcTest(AccountController.class)   // 이 컨트롤러와 웹 설정만 띄운다
class AccountControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccountService accountService;   // 진짜 서비스 대신 가짜

    @Test
    @DisplayName("없는 계좌 조회 -> 404 ACCOUNT_NOT_FOUND")
    void notFound() throws Exception {
        given(accountService.getAccount("9999-000-000000"))
                .willThrow(new AccountNotFoundException("9999-000-000000 계좌를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/accounts/9999-000-000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));   // 예외가 올바른 HTTP 상태 · 코드로 바뀌는가
    }

    @Test
    @DisplayName("계좌 조회 성공 → 200 과 계좌 정보")
    void findOne_ok() throws Exception {
        Account account = Account.open(1, "1111-111-111111", "입출금");
        account.deposit(new BigDecimal("50000.00"));
        given(accountService.getAccount(account.getAccountNo())).willReturn(account);

        mockMvc.perform(get("/api/accounts/" + account.getAccountNo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50000.00))
                .andExpect(jsonPath("$.status").value("정상"));
    }
}
```

- `get(...)` · `post(...)` 는 `MockMvcRequestBuilders`, `status()` · `jsonPath()` 는 `MockMvcResultMatchers` 에서 static import 합니다.

## 🧪 ⑤ 통합: 앱 전체가 뜨는가 (`WonitApplicationTests`)

```java
@SpringBootTest
class WonitApplicationTests {
    @Test
    void contextLoads() { }   // 앱이 뜨는지만 확인. 설정 오타 · Bean 연결 오류를 잡아 준다
}
```

- 기본 설정이 이미 H2 메모리 DB 라서 MySQL 없이 통과합니다.

## 🪵 실행 · 확인

```bash
./gradlew test
```

- 결과 보고서: `build/reports/tests/test/index.html` → IntelliJ 에서 오른쪽 클릭 → **Open In → Browser → Chrome**
- IntelliJ 에서는 테스트 클래스 · 메서드 왼쪽의 ▶ 로 하나씩 돌릴 수 있고, 결과 창의 **Export Test Results** 로 HTML 을 저장할 수 있습니다.
- 지금 코드의 결과: **테스트 16개 중 15개 통과, 1개 건너뜀**(`TestLifeCycle.test3`, `@Disabled`). `FetchStrategyTest` · `LazyLoadingTryTest` 는 [reference/fetch-strategy.md](reference/fetch-strategy.md) 용 테스트입니다.

| 테스트 클래스 | 개수 |
|---|---|
| `model/AccountTest` | 3 |
| `service/AccountServiceTest` | 1 |
| `repository/AccountRepositoryTest` | 1 |
| `controller/AccountControllerTest` | 2 |
| `WonitApplicationTests` | 1 |
| `MyTest` | 1 |
| `TestLifeCycle` | 3 (1개 건너뜀) |
| `FetchStrategyTest` · `LazyLoadingTryTest` | 3 · 1 |

## ✍️ 바로 해보기

1. `AccountTest` 에 **지급정지 계좌는 출금할 수 없다** 테스트를 추가하세요 (`account.setStatus("지급정지")` → `AccountFrozenException`, 잔액은 그대로)
2. `AccountTest` 의 `isEqualByComparingTo("70000")` 을 `isEqualTo(new BigDecimal("70000"))` 로 바꿔 돌려 보세요. 왜 실패하나요?
3. `AccountRepositoryTest` 에서 주석 처리된 `1002-345-678901` 줄로 바꿔 돌려 보고, 실패 메시지를 읽어 보세요

<details>
<summary>정답 보기</summary>

1.
```java
@Test
@DisplayName("지급정지 계좌는 출금할 수 없다")
void withdraw_frozen() {
    Account account = Account.open(1, "1002-111-111111", "입출금");
    account.deposit(new BigDecimal("10000"));
    account.setStatus("지급정지");

    assertThatThrownBy(() -> account.withdraw(new BigDecimal("1000")))
            .isInstanceOf(AccountFrozenException.class);
    assertThat(account.getBalance()).isEqualByComparingTo("10000");
}
```
2. 잔액은 `0.00` 에서 시작해 계산하므로 `70000.00` 입니다. `isEqualTo` 는 `equals` 라서 `70000` 과 `70000.00` 을 **다르다**고 봅니다.
3. `IncorrectResultSizeDataAccessException`: 시드에 같은 번호가 이미 있어서 조회 결과가 2건이 됐습니다.

</details>

---

# ✍️ 바로 해보기 (1 · 6 · 7절)

1. **PUT 에서 칸이 빠지면 400 으로 막기**: `AccountCreateRequest` 의 `customerId` 에 `@NotNull`, `accountType` 에 `@NotBlank` 를 붙이고, `AccountController.updateOne` 의 `@RequestBody` 앞에 `@Valid` 를 붙입니다. 1절 ② 요청이 400 이 되는지 확인합니다.
2. **변경 감지 확인**: `patchAccount` 의 `return accountRepository.save(account);` 를 `return account;` 로 바꿔도 PATCH 가 DB 에 반영되는지 확인합니다. 그다음 `@Transactional` 을 지워 보고 어떤 에러가 나는지 봅니다 (끝나면 되돌리기).
3. **읽기에 잠금 빼기**: `getTransactions()` 의 `lockAccount(accountNo)` 를 `getAccount(accountNo)` 로 바꾸고, 거래내역 조회 SQL 에서 `for update` 가 사라지는지 봅니다.

<details>
<summary>2번 답</summary>

- `save()` 를 빼도 반영됩니다. `@Transactional` 안에서 조회한 엔티티라서 끝날 때 변경 감지가 `UPDATE` 를 보냅니다.
- `@Transactional` 을 지우면 `lockAccount()` 의 `FOR UPDATE` 조회에서 `TransactionRequiredException` 이 나고 500 이 됩니다.

</details>

---

# 🏁 오늘의 미션

## 코드 리뷰: 이상한 곳 찾기 (5개 이상)

⏱ 10~15분. **코드는 고치지 않고 찾기만** 합니다.

지금 코드를 **실행해 보고 읽으면서** 이상한 곳을 5개 이상 찾아 한 줄씩 적어 보세요. 아래 힌트를 따라가면 됩니다.

- 응답에 **나오면 안 될 칸**이 나오는 API는 없나요?
- 1절에서 PUT 으로 칸을 지운 **그 계좌**로 `GET /api/accounts/report2` 를 불러 보세요.
- 없는 계좌 `9999` 를 `GET` 으로 조회할 때와 `PUT` · `PATCH` 로 보낼 때의 에러 **문장**을 비교해 보세요.
- 거래내역을 **읽기만** 하는데 하는 일이 과하지 않나요? (7절)
- 콘솔에 같은 SQL 이 몇 번씩 찍히나요?
- 주석이 지금 코드와 맞는지 읽어 보세요. (1절 · 6절)
- 2절을 고쳤다면, 해지한 계좌에 `PATCH` 를 보내 보세요.

<details>
<summary>정답 보기 (후보 8개)</summary>

1. `GET /api/accounts/balance` 만 엔티티를 그대로 돌려줘서 `accountId` · `customerId` 가 응답에 나옵니다. 다른 API 는 `AccountResponse` 를 씁니다.
2. PUT 에서 칸이 빠지면 주석은 "400" 이라고 하는데, 실제로는 200 + null 로 덮입니다 (1절).
3. 그 상태에서 `GET /api/accounts/report2` 는 **500** 입니다. `accountType` 이 null 이라 `.equals` 에서 `NullPointerException` 이 납니다. 고치기: `"적금".equals(account.getAccountType())`.
4. `GET /api/accounts/9999` 의 메시지는 `9999계좌를 찾을 수 없습니다` (띄어쓰기 · 마침표 없음) 인데, PUT · PATCH 경로(`lockAccount`)는 `9999 계좌를 찾을 수 없습니다.` 입니다. 같은 404 인데 문장이 둘입니다.
5. 거래내역 조회(`getTransactions`)는 읽기만 하는데 `FOR UPDATE` 잠금을 겁니다 (7절).
6. SQL 이 콘솔에 두 번씩 찍힙니다. `show-sql: true` 와 `org.hibernate.SQL: DEBUG` 를 둘 다 켰기 때문입니다. 하나만 남기면 됩니다.
7. PATCH 주석 "변경 감지가 없으므로 save()" 는 `@Transactional` 을 붙이기 **전** 설명입니다 (6절).
8. Soft Delete 로 고친 뒤에도 해지 계좌를 PATCH 로 되살릴 수 있습니다 (2절 체크포인트).

</details>

---

# ⚡ 5분 퀴즈

1. PUT 으로 보낼 때 빠진 칸은 어떻게 되고, PATCH 로 보낼 때 빠진 칸은 어떻게 되나요?
2. `Content-Type` 헤더 없이 JSON 을 POST 하면 몇 번이 오나요?
3. `new BigDecimal(1.005)` 가 위험한 이유는?
4. `@Transactional` 메서드에서 `RuntimeException` 이 나면 어떻게 되나요? 예외를 `catch` 로 삼키면 롤백될까요?
5. 거래내역 조회의 `page` 는 0 부터일까요, 1 부터일까요?

<details>
<summary>정답 보기</summary>

1. PUT 은 빠진 칸이 **null 로 덮이고**, PATCH 는 빠진 칸을 **그대로 둡니다**.
2. **415** (Unsupported Media Type).
3. `double` 값을 받아 만들기 때문입니다. 1.005 는 사실 1.00499999… 라서 반올림하면 1.01 이 아니라 **1.00** 이 됩니다. 문자열 `"1.005"` 로 만들어야 합니다.
4. 전부 **롤백**됩니다. 하지만 예외를 `catch` 로 삼키면 `@Transactional` 이 실패를 모르므로 **롤백되지 않습니다**.
5. **0 부터**입니다.

</details>

---

# ✅ 오늘의 정리

- [ ] PUT 은 통째로 교체(빠진 칸은 null), PATCH 는 보낸 칸만 수정이라는 것을 우리 응답으로 설명할 수 있다
- [ ] DELETE 가 행을 지우면 FK 에 막히거나 기록이 사라진다는 것을 보고, Soft Delete(상태 = 해지 · 잔액 있으면 409)로 고칠 수 있다
- [ ] 거래내역을 `page` · `size` 로 한 쪽씩 받고, `totalPages` · `last` 를 읽을 수 있다 (page 는 0부터, 정렬은 서버가 고정)
- [ ] Header(겉면)와 Body(속지)를 구분하고, `Content-Type` 이 빠지면 415 가 나는 것을 확인했다
- [ ] 돈에 `double` 을 쓰면 안 되는 이유와 BigDecimal 규칙 네 가지(문자열 생성 · 메서드 계산 · 다시 대입 · compareTo)를 안다
- [ ] `@Transactional` 의 한 묶음(롤백)과 변경 감지를 로그와 응답으로 확인했다
- [ ] `FOR UPDATE` 가 무엇을 막는지(Lost Update), 왜 트랜잭션 안에서만 되는지 설명할 수 있다
- [ ] 단위(도메인 · 서비스) · 슬라이스(저장소 · 웹) · 통합 테스트를 우리 파일로 구분하고, `./gradlew test` 보고서를 열었다

| 용어 | 한 줄 뜻 |
|---|---|
| 멱등 | 같은 요청을 여러 번 보내도 결과가 같다 (Day 1 에서 자세히) |
| 변경 감지 (dirty checking) | 트랜잭션 안에서 바꾼 엔티티를 JPA 가 찾아 자동으로 UPDATE |
| 롤백 | 트랜잭션 안의 작업을 전부 취소 |
| 프록시 | `@Transactional` 을 동작시키려고 스프링이 감싼 대리 객체 |
| Lost Update | 동시에 고치다가 한쪽 결과가 덮여 사라지는 현상 |
| 비관적 락 | 미리 잠가서 다른 요청을 기다리게 하는 방식 (`FOR UPDATE`) |
| 데드락 | 서로 상대의 잠금을 기다리며 둘 다 멈춘 상태 |
| Soft Delete | 행을 지우지 않고 상태만 바꿔 "삭제" 를 표현 (↔ Hard Delete) |
| 외래키 (FK) | 다른 표의 행을 가리키는 칸. 가리켜지는 행은 함부로 지울 수 없다 |
| 페이징 | 많은 데이터를 `page` · `size` 로 한 쪽씩 나눠 주는 것. `page` 는 0부터 |
| `Pageable` | 스프링이 `?page=&size=` 를 바꿔 주는 "몇 번째 쪽, 몇 건" 상자 |
| Header / Body | 요청 · 응답의 겉면(정보) / 속지(내용) |
| 415 | 서버가 읽을 수 없는 `Content-Type` 이라 거절 |
| Mock | 테스트용 가짜 객체. 대본(`given`)대로만 답한다 |
| 슬라이스 테스트 | 앱 전체 대신 한 층(저장소 · 웹)만 띄우는 테스트 |

# 🎤 면접 질문

**Q1. PUT 과 PATCH 의 차이는?**

<details>
<summary>답변 예시</summary>

"PUT 은 자원을 보낸 내용으로 통째로 교체하고, PATCH 는 보낸 칸만 수정합니다. 우리 프로젝트에서 PUT 에 계좌 종류를 빼고 보내면 그 칸이 null 로 덮였습니다. 그래서 상태 변경처럼 일부만 고치는 기능은 PATCH 로 만들고, PUT 은 모든 칸을 검증합니다."

</details>

**Q2. 돈을 왜 BigDecimal 로 다루나요?**

<details>
<summary>답변 예시</summary>

"double 은 2진수라 0.1 같은 값을 정확히 저장하지 못해 0.1 + 0.2 가 0.30000000000000004 가 됩니다. BigDecimal 은 10진수로 정확히 계산합니다. 대신 문자열로 생성하고, 불변이라 결과를 다시 대입하고, 비교는 equals 가 아니라 compareTo 를 씁니다."

</details>

**Q3. @Transactional 이 하는 일과, 동작하지 않는 경우는?**

<details>
<summary>답변 예시</summary>

"메서드 안의 DB 작업을 한 묶음으로 커밋하거나 RuntimeException 이 나면 전부 롤백합니다. 또 트랜잭션 안에서 바꾼 엔티티는 변경 감지로 save 없이 UPDATE 됩니다. 프록시로 동작하기 때문에 같은 클래스 안에서 this 로 부르면 적용되지 않고, 예외를 catch 로 삼키거나 checked 예외를 던지면 기본 설정에서는 롤백되지 않습니다."

</details>

**Q4. 비관적 락과 낙관적 락의 차이는? 잔액에는 무엇을 쓰나요?**

<details>
<summary>답변 예시</summary>

"비관적 락은 읽을 때 SELECT FOR UPDATE 로 잠가서 다른 요청을 기다리게 하고, 낙관적 락은 @Version 으로 저장 시점에 충돌을 감지해 실패시킵니다. 잔액은 충돌이 잦고 실패하면 안 되므로 비관적 락을 썼습니다. 잠금은 트랜잭션이 끝날 때 풀리므로 반드시 트랜잭션 안에서 쓰고, 계좌 두 개를 잠글 때는 데드락을 막으려고 항상 계좌번호 순서로 잠급니다."

</details>

**Q5. 요청의 Header 와 Body 는 어떻게 다르고, Content-Type 은 왜 필요한가요?**

<details>
<summary>답변 예시</summary>

"Header 는 요청에 대한 정보(형식, 인증 토큰, 멱등 키 등)이고 Body 는 실제 내용입니다. 서버는 Content-Type 을 보고 Body 를 어떻게 읽을지 정하므로, 우리 프로젝트에서 Content-Type 없이 JSON 을 보내면 415 Unsupported Media Type 으로 거절됐습니다."

</details>

**Q6. 단위 테스트와 통합 테스트는 어떻게 나눠 쓰나요? Mock 은 왜 쓰나요?**

<details>
<summary>답변 예시</summary>

"규칙이 많은 도메인 로직은 Spring 없이 단위 테스트로 촘촘히, 서비스는 Mockito 로 저장소를 가짜로 바꿔 흐름만 빠르게 확인합니다. 저장소와 컨트롤러는 @DataJpaTest · @WebMvcTest 로 한 층만 띄워 확인하고, 앱 전체가 뜨는지는 @SpringBootTest 하나로 봅니다. 작은 테스트는 많이, 큰 테스트는 적게 두는 테스트 피라미드입니다. Mock 은 DB 같은 느리고 외부적인 것을 대본대로 답하는 가짜로 바꿔, 테스트하려는 코드만 빠르고 안정적으로 확인하기 위해 씁니다."

</details>

**Q7. 계좌 해지 API 는 행을 지우나요?**

<details>
<summary>답변 예시</summary>

"지우지 않습니다. 거래 기록은 법적으로 보관해야 하고, 거래 표가 계좌를 외래키로 가리키고 있어 지우려 해도 FK 에 막힙니다. 그래서 DELETE 요청을 받으면 상태만 '해지' 로 바꾸는 Soft Delete 로 만들고, 잔액이 남아 있으면 409 로 거절합니다. 같은 해지 요청을 두 번 보내도 결과가 같아서 멱등이기도 합니다."

</details>
