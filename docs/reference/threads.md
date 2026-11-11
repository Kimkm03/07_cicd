# 읽을거리. 스레드와 동시성: 은행 창구로 이해하는 멀티스레드 (선택)

- **선택 읽을거리**입니다. `AccountService` 의 `lockAccount()`(비관적 락)와 `transfer()` 를 읽다가 "스레드가 정확히 뭐지?", "왜 락이 필요하지?" 싶을 때 보세요. 읽지 않아도 수업을 따라가는 데 문제없습니다.
- 이번 과정에서는 스레드 100개로 락 없는 동시성 버그를 재현하는 실험이나 데드락 실험은 하지 않습니다. 이 자료는 그 배경이 되는 개념만 설명합니다.
- 각 절은 **개념 → ❌ 우리 코드에서 찾기 → ✅ 고치기 → 🏦 은행에서는 → ✍️ 바로 해보기** 순서입니다.
- 💻 예제는 대부분 **`main` 메서드 하나로 바로 실행**할 수 있는 순수 자바 코드입니다. Spring 없이도 돌아갑니다.

> 문법은 AI가 대신 써 줍니다. 하지만 **"이 값은 여러 스레드가 같이 쓰는가, 혼자 쓰는가"** 는 여러분이 판단해야 합니다.

> 💡 **스레드(Thread)**<br>
> 프로그램 안에서 **실제로 일을 하는 일꾼 한 명**입니다. 은행 지점(프로그램) 안에 창구 직원(스레드)이 여러 명 있어서, 손님 여러 명을 **동시에** 응대할 수 있습니다.

## 🎯 읽고 나면 할 수 있는 것

1. 프로세스와 스레드의 차이를 **은행 지점과 창구 직원**으로 설명할 수 있다
2. Spring Boot 서버에서 **요청 하나가 스레드 하나**에 배정된다는 것을 로그로 확인한다
3. 스레드끼리 **공유하는 것(힙)** 과 **각자 가진 것(스택)** 을 구분한다
4. 경쟁 상태(Race Condition)가 생기는 순간을 **시간표**로 그릴 수 있다
5. `synchronized` 로는 은행 서버의 동시성 문제를 **왜 다 못 막는지** 설명할 수 있다

## 🤔 왜 필요할까요?

`AccountService` 의 입금 · 이체는 계좌를 읽자마자 `lockAccount()` 로 잠급니다. 락이 없으면 같은 계좌에 요청이 동시에 들어왔을 때 일부 변경이 잔액에 반영되지 않는 문제(Lost Update)가 생길 수 있기 때문입니다. 그런데 이런 질문이 남죠.

### 😫 이런 궁금증이 생겨요

**궁금증 1: "스레드가 100개면 서버도 100대인가요?"**

아닙니다. 서버(프로그램)는 하나이고, 그 안에서 일꾼만 여러 명입니다.

**궁금증 2: "AccountService 는 하나뿐인데, 손님 100명이 어떻게 동시에 써요?"**

직원 100명이 **업무 매뉴얼 한 권**을 같이 보면서 각자 자기 손님을 응대하는 것과 같습니다. 매뉴얼(코드)은 하나여도 일하는 사람은 여럿이에요.

**궁금증 3: "자바에는 `synchronized` 가 있다던데, 그거 한 줄이면 해결 아닌가요?"**

컴퓨터 한 대에서는 어느 정도 맞습니다. 그런데 은행 서버는 보통 **여러 대**가 동시에 돌아가고, 진짜 잔액은 **DB** 에 있습니다. 그래서 자바 안의 잠금만으로는 부족해요. 7절에서 직접 확인합니다.

## ⏱ 이 자료의 흐름

| 이 자료의 절 | 한 줄 목표 |
|---|---|
| 1 | 프로세스 vs 스레드 — 지점과 창구 직원 |
| 2 | Spring Boot 는 요청마다 스레드를 배정한다 — 로그의 `http-nio-8080-exec-3` 읽기 |
| 3 | 스레드가 공유하는 것(힙)과 각자 가진 것(스택) |
| 4 | 경쟁 상태 — 순수 자바로 돈이 사라지는 장면 재현 |
| 5 | 자바 안에서 막는 법 — `synchronized` · `AtomicLong` |
| 6 | 스레드 풀 · `ExecutorService` · `CountDownLatch` — 동시 실행 테스트 코드 읽는 법 |
| 7 | 왜 은행 서버는 `synchronized` 로 부족한가 — DB 락이 필요한 이유 |
| 8 | `ThreadLocal` — MDC 와 `@Transactional` 이 스레드별로 따로인 이유 |
| 9 | 톰캣 스레드 · DB 커넥션 풀 · 교착상태 |
| 10 | 체크리스트 · 용어집 · 면접 질문 |

---

# 1. 프로세스 vs 스레드 — 지점과 창구 직원

### 💡 개념

| | 은행으로 치면 | 컴퓨터에서 |
|---|---|---|
| **프로세스** | **지점 하나** (건물 · 금고 · 서류함을 따로 가짐) | 실행 중인 프로그램 하나. `java -jar wonit.jar` 를 실행하면 프로세스 1개 |
| **스레드** | 지점 안의 **창구 직원** | 프로세스 안에서 실제로 코드를 실행하는 일꾼. 한 프로세스에 여러 명 |

- 지점이 다르면 금고도 따로라서 서로의 돈을 못 봅니다 → **프로세스끼리는 메모리를 공유하지 않습니다**
- 같은 지점의 직원들은 **같은 금고 · 같은 서류함**을 씁니다 → **스레드끼리는 메모리(힙)를 공유합니다**

> 💡 **핵심**<br>
> 동시성 문제는 **같은 지점의 직원들(스레드)이 같은 금고(공유 메모리 · DB)를 동시에 만질 때** 생깁니다.

### ✍️ 바로 해보기

IntelliJ 에서 서버를 띄운 상태로 터미널에 `jps` 를 입력해 보세요. `WonitApplication` 이 몇 개 보이나요?

<details><summary>정답 보기</summary>

하나입니다. 프로세스는 하나이고, 그 안에 스레드가 수십 개 있습니다. `jstack <PID>` 를 입력하면 스레드 목록이 쭉 나오는데, 그중 `http-nio-8080-exec-1` ~ `exec-10` 같은 이름이 손님 요청을 처리하는 창구 직원입니다.

</details>

---

# 2. Spring Boot 는 요청마다 스레드를 배정한다

### 💡 개념

Spring Boot 안에는 **톰캣(Tomcat)** 이라는 웹 서버가 들어 있습니다. 톰캣은 손님(요청)이 오면 **대기 중인 창구 직원(스레드) 한 명을 배정**해서 그 요청을 처음부터 끝까지 처리하게 합니다. 이것을 **요청당 스레드(thread-per-request)** 모델이라고 부릅니다.

```
손님 A ──▶ [http-nio-8080-exec-1] ── Controller → Service → Repository → 응답
손님 B ──▶ [http-nio-8080-exec-2] ── Controller → Service → Repository → 응답   ← 동시에!
손님 C ──▶ [http-nio-8080-exec-3] ── ...
```

- 톰캣의 창구 직원은 기본 **최대 200명** (`server.tomcat.threads.max`, 기본값 200)
- 일이 끝난 직원은 퇴근하지 않고 **다음 손님을 기다립니다** (스레드 재사용). 이게 8절 MDC(요청 추적 번호) 이야기의 복선이에요

### ❌ 우리 코드에서 찾기

서버 콘솔의 로그 한 줄에는 스레드 이름이 함께 찍힙니다. `t6-react` 를 실행해 실제로 찍힌 줄입니다.

```
2026-10-18T12:57:29.790Z  WARN 7645 --- [wonit] [nio-8080-exec-9] [c08c5fd1923dfb15385c652437e18c40-2a97f3f3ab96eb51] c.e.wonit.common.GlobalExceptionHandler  : 지급정지 계좌 출금 시도: ...
2026-10-18T12:57:29.941Z DEBUG 7645 --- [wonit] [nio-8080-exec-5] [f087e5f29e5ed47bd5f5440d809d5ae7-48cbde6bdb6b671b] o.s.orm.jpa.JpaTransactionManager        : Creating new transaction ...
```

| 조각 | 뜻 |
|---|---|
| `nio-8080-exec-9` | 스레드 이름. 로그 칸이 15자라 앞부분(`http-`)이 잘려 보인다. 여기서 `8080` 은 서버를 띄운 포트다 |
| `nio` | 톰캣의 처리 방식(Non-blocking I/O) 이름 |
| `exec-9` | **9번 창구 직원** |
| `[c08c5fd1…-2a97f3f3…]` | 요청 번호(traceId)와 구간 번호(spanId). 요청마다 다르다 (Day 2 요청 추적) |

위 두 줄은 **서로 다른 요청**이라 스레드 이름(`exec-9`, `exec-5`)도 요청 번호도 다릅니다. 로그가 섞여도 요청 번호로 어느 줄이 어느 요청인지 가려집니다.

### ✍️ 바로 해보기

`api.http` 에서 같은 GET 요청(예: 계좌 목록)을 5번 연달아 보내고, 로그의 스레드 이름을 적어 보세요. 매번 같은 직원인가요?

<details><summary>정답 보기</summary>

매번 다를 수도, 같을 수도 있습니다. 쉬고 있는 직원 아무나 배정되기 때문이에요. 중요한 건 **한 요청 안의 로그는 처음부터 끝까지 같은 스레드 이름**이라는 점입니다. 요청 하나 = 직원 한 명이 끝까지 책임집니다.

</details>

> 이 부분은 심화입니다. **못 따라와도 괜찮습니다.**<br>
> Java 21 에는 **가상 스레드(Virtual Thread)** 가 있습니다. 진짜 직원(OS 스레드) 몇 명이 수많은 "가상 직원" 의 일을 번갈아 처리하는 방식이라, 기다리는 시간이 긴 일(DB · 외부 API 호출)이 많을 때 훨씬 많은 손님을 받을 수 있습니다. Spring Boot 에서는 `spring.threads.virtual.enabled: true` 한 줄로 켭니다. 켜면 로그의 스레드 이름 모양도 바뀌니 직접 확인해 보세요.

---

# 3. 스레드가 공유하는 것 · 각자 가진 것

### 💡 개념

> 💡 **스택(Stack) 과 힙(Heap)**<br>
> **스택** = 직원 한 명의 **개인 메모장**. 메서드의 매개변수 · 지역 변수가 여기 적힙니다. 다른 직원은 못 봅니다.<br>
> **힙** = 지점의 **공용 서류함**. `new` 로 만든 객체와 그 필드가 여기 있습니다. 모든 직원이 같이 봅니다.

```
              ┌───────── 프로세스 (지점) ─────────┐
              │                                     │
 [exec-1 스택]│ amount=10000, account(주소)  ─┐     │
 [exec-2 스택]│ amount=20000, account(주소)  ─┼──▶ 힙: AccountService (딱 1개, 싱글톤)
 [exec-3 스택]│ amount=5000,  account(주소)  ─┘        Account 객체들, ...
              └─────────────────────────────────────┘
```

| 무엇 | 어디에 | 스레드끼리 공유? | 안전? |
|---|---|---|---|
| 메서드의 지역 변수 · 매개변수 | 스택 | ❌ 각자 따로 | ✅ 안전 |
| Bean(Service · Repository)의 **필드** | 힙 | ✅ 같이 씀 | ⚠️ 위험 (값을 바꾸면) |
| `static` 필드 | 힙(메서드 영역) | ✅ 같이 씀 | ⚠️ 위험 |
| DB 의 행 | DB | ✅ 모든 스레드 · 모든 서버가 같이 씀 | ⚠️ 가장 위험 |

### ❌ 우리 코드에서 찾기

```java
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;   // 필드지만 final + 바뀌지 않음 → ✅ 안전

    public Transaction deposit(String accountNo, DepositRequest request) {   // 매개변수는 스택 → ✅ 안전
        Account account = lockAccount(accountNo);   // 지역 변수 account 는 스택. 단, 가리키는 계좌 "데이터" 는 DB 에서 공유!
        ...
    }
}
```

Spring Bean 은 기본이 **싱글톤** — 서버 전체에 딱 하나만 만들어서 모든 스레드가 같이 씁니다. 그래서 이런 코드는 사고입니다.

```java
@Service
public class AccountService {
    private BigDecimal lastBalance;   // 🚨 요청마다 다른 값을 필드에 담으면 직원들이 같은 칸에 덮어쓴다

    public BigDecimal deposit(String accountNo, BigDecimal amount) {
        lastBalance = ...;           // exec-1 이 쓴 값을 exec-2 가 덮어쓰고
        return lastBalance;          // exec-1 이 exec-2 손님의 잔액을 돌려준다 😱
    }
}
```

### ✅ 고치기

> **Service · Controller · Repository 는 무상태(Stateless)로.** 요청마다 달라지는 값은 **필드가 아니라 지역 변수**에 둡니다. 필드는 주입받은 부품(`final`)만.

### 🏦 은행에서는

"A 손님 화면에 B 손님 잔액이 보였다" 는 장애는 실제로 일어나는 사고이고, 원인의 상당수가 **싱글톤 객체의 필드에 요청별 값을 담은 코드**입니다. 개인정보 유출로 이어지기 때문에 코드 리뷰에서 가장 먼저 보는 항목 중 하나입니다.

---

# 4. 경쟁 상태 — 순수 자바로 돈이 사라지는 장면

### 💡 개념

> 💡 **경쟁 상태(Race Condition)**<br>
> 여러 스레드가 **같은 값을 동시에 읽고-계산하고-쓸 때**, 실행 **순서(타이밍)에 따라 결과가 달라지는** 상황입니다. 결과가 매번 다르다는 게 가장 무서운 점이에요.

`balance = balance - 10000` 은 한 줄처럼 보이지만, 컴퓨터는 **세 단계**로 나눠 합니다.

1. **읽기**: 금고에서 잔액을 꺼내 본다
2. **계산**: 10000 을 뺀다
3. **쓰기**: 계산한 값을 금고에 적는다

두 직원이 이 세 단계를 **번갈아** 하면:

| 시간 | 직원 A (exec-1) | 직원 B (exec-2) | 금고의 잔액 |
|---|---|---|---|
| 1 | 읽기: 100,000 | | 100,000 |
| 2 | | 읽기: 100,000 | 100,000 |
| 3 | 계산: 90,000 | | 100,000 |
| 4 | | 계산: 90,000 | 100,000 |
| 5 | 쓰기: 90,000 | | **90,000** |
| 6 | | 쓰기: 90,000 | **90,000** 😱 |

2만 원이 나갔는데 잔액은 1만 원만 줄었습니다. B 가 A 의 결과를 **덮어썼기** 때문이에요. 이것이 **갱신 손실(Lost Update)** 입니다. `AccountService` 가 계좌를 `lockAccount()` 로 잠그고 시작하는 이유입니다.

### ✍️ 바로 해보기 — 순수 자바로 재현하기

Spring 도 DB 도 없이, 아래 파일 하나만 실행해 보세요.

```java
// RaceDemo.java — IntelliJ 에서 main 옆의 ▶ 버튼으로 실행
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RaceDemo {

    static long balance = 1_000_000;   // 공용 금고 (static = 모든 스레드가 같이 봄)

    static void withdraw(long amount) {
        long read = balance;           // ① 읽기
        long next = read - amount;     // ② 계산
        Thread.onSpinWait();           //    (끼어들 틈을 조금 더 만든다)
        balance = next;                // ③ 쓰기
    }

    public static void main(String[] args) throws InterruptedException {
        int count = 100;
        ExecutorService pool = Executors.newFixedThreadPool(32);   // 직원 32명
        CountDownLatch done = new CountDownLatch(count);           // 100건 끝날 때까지 기다리는 카운터

        for (int i = 0; i < count; i++) {
            pool.submit(() -> {
                withdraw(10_000);
                done.countDown();
            });
        }
        done.await();
        pool.shutdown();

        System.out.println("기대: 0원, 실제: " + balance + "원");
    }
}
```

몇 번 실행해 보세요. 결과가 어떤가요?

<details><summary>정답 보기</summary>

실행할 때마다 **결과가 다르고**, 0원이 아닌 값(예: 30,000원, 120,000원 …)이 자주 나옵니다. 운이 좋으면 0원이 나올 때도 있어요. 이게 경쟁 상태가 무서운 이유입니다. **"내 PC 에서 한 번 해 봤더니 됐어요" 는 아무것도 증명하지 못합니다.** 그래서 동시성은 "한 번 돌려 보기" 가 아니라 락 같은 장치로 **설계 단계에서 막아야** 합니다.

> 이 `RaceDemo` 는 메모리 안의 변수 하나라서 "돈이 틀어지는" 증상만 보입니다. 진짜 DB 와 JPA 에서는 거기에 더해 **일부 요청이 데드락 에러로 실패하는** 증상이 나올 수도 있습니다 (이유는 아래 9절. 이 과정의 H2 에서는 재현하지 않고 설명만 합니다).

</details>

### 💡 핵심

> **"읽기 → 계산 → 쓰기" 사이에 다른 스레드가 끼어들 수 있으면, 그 코드는 동시에 실행될 때 틀릴 수 있습니다.**

---

# 5. 자바 안에서 막는 법 — `synchronized` · `AtomicLong`

### 💡 개념

> 💡 **임계 영역(Critical Section)**<br>
> "한 번에 한 직원만 들어가야 하는 구역" 입니다. 4절의 읽기-계산-쓰기 세 줄이 바로 임계 영역이에요.

| 방법 | 비유 | 코드 |
|---|---|---|
| `synchronized` | 금고 방에 **열쇠가 하나**. 열쇠를 가진 직원만 들어감 | `static synchronized void withdraw(...)` |
| `AtomicLong` | 읽기-계산-쓰기를 **한 동작으로** 해 주는 특수 금고 | `balance.addAndGet(-amount)` |
| `ConcurrentHashMap` | 여러 직원이 동시에 써도 안전한 서류함 | `new ConcurrentHashMap<>()` |

### ✅ 고치기

```java
// 방법 1: synchronized — 한 번에 한 스레드만 이 메서드에 들어온다
static synchronized void withdraw(long amount) {
    long read = balance;
    long next = read - amount;
    balance = next;
}

// 방법 2: AtomicLong — 읽기·계산·쓰기를 쪼개지 않고 한 번에
static final AtomicLong balance = new AtomicLong(1_000_000);
static void withdraw(long amount) {
    balance.addAndGet(-amount);
}
```

### ✍️ 바로 해보기

4절의 `RaceDemo` 에 `synchronized` 를 붙이고 다시 여러 번 실행해 보세요.

<details><summary>정답 보기</summary>

몇 번을 돌려도 항상 `실제: 0원` 이 나옵니다. 대신 직원들이 열쇠를 기다리느라 **줄을 서게** 됩니다. 안전과 속도를 맞바꾼 거예요. 이 "기다림" 은 응답시간이 길어지는 모습으로 나타납니다.

</details>

---

# 6. 스레드 풀 · `ExecutorService` · `CountDownLatch` — 동시 실행 테스트 코드 읽는 법

### 💡 개념

> 💡 **스레드 풀(Thread Pool)**<br>
> 일이 올 때마다 직원을 새로 뽑으면 채용 비용이 큽니다. 그래서 **직원을 미리 N명 뽑아 두고** 일감을 나눠 줍니다. 톰캣의 200명도 스레드 풀이에요.

스레드 100개로 같은 계좌에 동시에 입금해 보는 테스트는 보통 이런 모양입니다. 한 줄씩 해석해 봅시다. (읽기용 예시이고, 이번 과정의 레포에는 들어 있지 않습니다.)

```java
int count = 100;                                             // 손님 100명
ExecutorService pool = Executors.newFixedThreadPool(32);     // 직원 32명을 미리 뽑은 지점
CountDownLatch latch = new CountDownLatch(count);            // "100" 에서 시작하는 카운트다운 전광판

for (int i = 0; i < count; i++) {
    pool.submit(() -> {                                      // 일감 100개를 지점에 맡긴다 (직원 32명이 나눠서 처리)
        try {
            accountService.deposit(accountNo, new DepositRequest(new BigDecimal("10000"), null));
            success.incrementAndGet();                       // 성공 횟수를 세어 둔다 (AtomicInteger: 여러 직원이 동시에 올려도 안 틀리는 숫자)
        } catch (Exception e) {
            failures.incrementAndGet();                      // 데드락 같은 실패도 세어 둔다
        } finally {
            latch.countDown();                               // 일 하나 끝날 때마다 전광판 -1 (예외가 나도 꼭! → finally)
        }
    });
}
latch.await(60, TimeUnit.SECONDS);                           // 전광판이 0 이 될 때까지 테스트(메인 스레드)는 기다린다 (최대 60초. 영원히 기다리지는 않는다)
```

| 코드 | 왜 필요한가 |
|---|---|
| `newFixedThreadPool(32)` | 진짜로 **동시에** 실행되게 하려면 스레드가 여럿 필요하다 |
| `submit(() -> ...)` | 람다 안의 코드가 **다른 스레드에서** 실행된다 |
| `latch.countDown()` 을 `finally` 에 | 입금이 예외(휴면 계좌 등)로 끝나도 카운트는 줄어야 한다. 안 그러면 `await()` 가 영원히 기다림 |
| `latch.await(60, SECONDS)` | 이게 없으면 입금이 다 끝나기도 전에 잔액을 검사해 버린다. 시간 제한을 두면 문제가 생겨도 테스트가 영원히 멈추지 않는다 |
| `AtomicInteger` 로 성공 · 실패 세기 | 락이 없을 때 "몇 건이 성공했고 몇 건이 실패했는지" 를 알아야 사라진 돈을 계산할 수 있다 (5절의 `AtomicLong` 과 같은 종류) |

### 🚨 이 테스트에 `@Transactional` 을 붙이면 안 되는 이유

`@Transactional` 이 붙은 테스트는 **테스트를 실행한 스레드(메인)** 에 트랜잭션을 걸고, 끝나면 롤백합니다. 그런데 입금은 **풀의 다른 스레드 32명**이 합니다. 트랜잭션은 스레드마다 따로라서(8절) 메인의 트랜잭션은 그 직원들에게 전달되지 않아요. 결국 "테스트 데이터는 메인 트랜잭션 안에만 있고(커밋 전이라 다른 스레드에겐 안 보임), 입금 스레드들은 그걸 못 찾는" 이상한 일이 벌어집니다.

### ✍️ 바로 해보기 — 출발 신호를 맞추면?

지금 테스트는 일감을 맡기는 순서대로 먼저 시작하는 스레드가 생깁니다. **모두 동시에 출발**하게 하려면 어떻게 할까요?

<details><summary>정답 보기</summary>

`CountDownLatch` 를 하나 더 **출발 신호**로 씁니다.

```java
CountDownLatch ready = new CountDownLatch(1);   // 출발 신호 (1 → 0 이 되는 순간 다 같이 출발)
CountDownLatch done  = new CountDownLatch(count);

for (int i = 0; i < count; i++) {
    pool.submit(() -> {
        try {
            ready.await();                       // 신호가 올 때까지 대기
            accountService.deposit(ACCOUNT_NO, new DepositRequest(new BigDecimal("10000"), null));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            done.countDown();
        }
    });
}
ready.countDown();   // 탕! 출발
done.await();
```

이렇게 하면 경쟁이 더 치열해져서 락이 없을 때 버그가 **더 확실하게** 재현됩니다. 단, 풀의 직원이 32명이므로 동시에 출발하는 건 최대 32명입니다.

</details>

---

# 7. 왜 은행 서버는 `synchronized` 로 부족한가

5절에서 `synchronized` 로 해결했으니, `AccountService.deposit()` 에도 붙이면 끝일까요?

### ❌ 함정 1 — `synchronized` + `@Transactional` 은 그래도 돈이 샌다

```java
@Transactional
public synchronized Transaction deposit(String accountNo, DepositRequest request) { ... }
```

`@Transactional` 은 스프링이 메서드 앞뒤를 감싸는 **프록시(비서)** 가 처리합니다. 그림으로 그리면 이렇습니다.

```
[비서: 트랜잭션 시작] → [synchronized 열쇠 획득 → deposit 본문 → 열쇠 반납] → [비서: commit]
                                                              ↑ 여기와 commit 사이에 틈이 있다!
```

직원 A 가 열쇠를 반납한 순간, 아직 **commit 은 안 된** 상태입니다. 그 틈에 직원 B 가 열쇠를 받아 DB 에서 잔액을 읽으면 **A 가 빼기 전의 잔액**을 읽습니다. 결국 Lost Update 가 그대로 납니다.

### ❌ 함정 2 — 서버가 두 대면 열쇠도 두 개

```
서버 1 (JVM) ── synchronized 열쇠 ①  ─┐
                                        ├──▶ 같은 DB 의 같은 계좌 행
서버 2 (JVM) ── synchronized 열쇠 ②  ─┘
```

`synchronized` 는 **JVM(프로세스) 하나 안에서만** 통하는 열쇠입니다. 은행 서버는 장애 대비로 보통 여러 대를 띄우는데, 서버 1 의 직원과 서버 2 의 직원은 **서로 다른 열쇠**를 들고 있으니 아무것도 막지 못합니다.

### ✅ 고치기 — 열쇠를 DB 에 둔다

> **진짜 잔액이 DB 에 있으니, 열쇠도 DB 에 둡니다.** `AccountService.lockAccount()` 가 쓰는 **비관적 락(`SELECT ... FOR UPDATE`)** 은 "이 계좌 행을 내가 commit 할 때까지 아무도 못 만지게" 를 **DB 가** 지켜 줍니다. 서버가 몇 대든, commit 시점이 언제든 상관없어요.

| | `synchronized` | DB 락 (`FOR UPDATE`) |
|---|---|---|
| 열쇠가 있는 곳 | JVM 한 개 안 | DB |
| 서버 여러 대 | ❌ 못 막음 | ✅ 막음 |
| commit 까지 보호 | ❌ 틈이 있음 | ✅ commit/rollback 때 풀림 |
| 쓰는 곳 | 메모리 안의 값(캐시 · 카운터) | **DB 에 있는 돈** |

### 🏦 은행에서는

은행 시스템에서 "자바 코드에 `synchronized` 를 붙여서 잔액 동시성을 해결했다" 는 코드 리뷰에서 통과하지 못합니다. 돈의 진실은 DB(원장)에 있고, 동시성 제어도 **원장을 기준**으로 해야 합니다.

### 💡 핵심

> **잠금은 "진짜 데이터가 있는 곳" 에 걸어야 합니다.** 메모리 안의 값이면 `synchronized` · `Atomic`, DB 의 값이면 DB 락.

---

# 8. `ThreadLocal` — MDC 와 `@Transactional` 이 스레드별로 따로인 이유

### 💡 개념

> 💡 **ThreadLocal**<br>
> **직원마다 따로 있는 개인 사물함**입니다. 같은 이름의 사물함(변수)이라도 직원마다 내용물이 다릅니다. 3절의 "공용 서류함(힙)" 과 반대예요.

이번 과정에서 이미 두 번 썼습니다.

| 어디서 | 사물함에 넣은 것 | 그래서 |
|---|---|---|
| 요청 추적(`t4-tracing`)의 **MDC** (추적 라이브러리가 넣고 비운다) | 요청 번호 `traceId` · 구간 번호 `spanId` | 같은 요청의 로그 줄에만 같은 번호가 찍힌다 |
| 입금 · 이체의 `@Transactional` | 지금 쓰는 **DB 커넥션 · 트랜잭션** | 한 요청 안의 Repository 호출들이 같은 트랜잭션으로 묶인다 |

### ❌ 우리 코드에서 찾기 — 사물함을 왜 꼭 비워야 하나

`RequestIdFilter` 에는 `MDC.put` 도 `MDC.clear()` 도 없습니다. 번호를 사물함에 넣고 요청이 끝나면 **비우는 일을 추적 라이브러리가 대신** 하기 때문이에요. 만약 직접 했다면 이런 모양이었을 겁니다.

```java
try {
    chain.doFilter(request, response);
} finally {
    MDC.clear();   // 🚨 이게 없으면?
}
```

2절에서 "일이 끝난 직원은 퇴근하지 않고 다음 손님을 받는다" 고 했죠? 사물함을 비우지 않으면 **다음 손님 응대 때 이전 손님의 요청 번호가 그대로 남아** 로그에 찍힙니다. 스레드 재사용 + ThreadLocal = **반드시 비우기**. (라이브러리가 해 주니 잊어버릴 일은 없지만, 왜 비우는지는 알고 있어야 합니다.)

### 🚨 새 스레드로 넘어가면 사물함은 따라가지 않는다

```java
@Transactional
public void transfer(...) {
    from.withdraw(...);
    new Thread(() -> to.deposit(...)).start();   // 🚨 새 직원에게 맡기면
}
```

새 스레드에는 **트랜잭션 사물함도, MDC 사물함도 없습니다.** 입금은 다른 트랜잭션(또는 트랜잭션 없음)에서 실행되고, 로그에는 요청 번호 칸이 비어서 찍힙니다. 6절처럼 `ExecutorService` 로 만든 `pool-1-thread-N` 스레드의 로그에서 요청 번호 칸이 비어 있는 것도 같은 이유예요. `@Async` 도 기본은 마찬가지입니다. (번호를 새 스레드로 이어 주는 설정이 따로 있지만 우리 프로젝트에는 적용하지 않았습니다.) 그래서 "한 묶음이어야 하는 일" 은 **같은 스레드에서** 처리합니다.

---

# 9. 톰캣 스레드 · DB 커넥션 풀 · 교착상태

### 💡 개념 — 직원은 200명인데 금고 열쇠는 10개

| | 기본값 | 비유 |
|---|---|---|
| 톰캣 스레드 (`server.tomcat.threads.max`) | 200 | 창구 직원 200명 |
| DB 커넥션 풀 (HikariCP `maximum-pool-size`) | 10 | 금고(DB)에 들어가는 **출입증 10장** |

직원이 200명이어도 출입증이 10장이면 DB 일은 **동시에 10명만** 합니다. 나머지는 출입증이 돌아올 때까지 **줄을 서요.** 그런데 비관적 락을 걸고 오래 기다리는 직원은 **출입증을 쥔 채로** 기다립니다. 그래서 락 대기가 길어지면 출입증이 바닥나고, DB 와 상관없는 조회 요청까지 느려집니다.

### 💡 교착상태(Deadlock) — 두 직원이 서로의 도장을 기다림

| 시간 | 직원 A: 1번 → 2번 계좌 이체 | 직원 B: 2번 → 1번 계좌 이체 |
|---|---|---|
| 1 | 1번 계좌 잠금 🔒 | 2번 계좌 잠금 🔒 |
| 2 | 2번 계좌 잠금 시도… B 가 쥐고 있어 대기 ⏳ | 1번 계좌 잠금 시도… A 가 쥐고 있어 대기 ⏳ |
| 3 | 영원히 대기 | 영원히 대기 |

MySQL 같은 DB 는 이걸 알아채고 한쪽을 강제로 실패시킵니다(`Deadlock found when trying to get lock`). 해결은 `AccountService.transfer()` 가 하는 대로 **항상 같은 순서로 잠그기** — 둘 다 "계좌번호가 작은 쪽 먼저" 잠그면, 2번 계좌를 먼저 잡는 일이 생기지 않습니다.

### 🚨 계좌 하나뿐인 출금에서도 데드락이 난다

위 표는 계좌가 **두 개**인 경우입니다. 그런데 **계좌 한 개**에 출금을 락 없이 동시에 보내도 데드락이 날 수 있습니다 (MySQL 에서 관찰되는 현상입니다. 이 과정의 H2 에서는 재현하지 않으니 설명으로만 읽으세요).

이유는 출금 · 입금이 계좌 잔액만 고치는 게 아니라 `transaction` 표에 **거래 기록도 INSERT** 하기 때문입니다.

1. `transaction` 표는 `account` 표를 가리키는 **외래키(FK)** 를 가집니다. 거래 한 줄을 넣을 때 MySQL 은 "그 계좌가 진짜 있나?" 를 확인하려고 해당 계좌 행에 **공유 락(읽기 팻말)** 을 붙입니다. 읽기 팻말은 여러 직원이 동시에 붙일 수 있습니다.
2. 그다음 잔액을 UPDATE 하려면 **독점 락(혼자 쓰는 락)** 이 필요한데, 남의 읽기 팻말이 붙어 있으면 얻을 수 없습니다.
3. 직원 A 와 B 가 둘 다 읽기 팻말을 붙인 채 서로 독점 락을 기다리면 → 서로 영원히 기다림 → MySQL 이 한쪽을 강제로 실패시킵니다.

즉 "서로 반대 순서로 잠그는" 경우가 아니어도, **"읽기 락을 먼저 잡고 나중에 독점 락으로 올리려는" 순서** 때문에 데드락이 생길 수 있습니다.

**비관적 락(`SELECT ... FOR UPDATE`)이 이걸 막는 이유:** 처음부터 **독점 락을 먼저** 잡기 때문입니다. 독점 락은 한 번에 한 명만 가질 수 있으니 나머지 직원은 문 밖에서 줄을 서고, 독점 락을 가진 직원 혼자 INSERT 와 UPDATE 를 끝냅니다. 읽기 팻말을 쥔 채 서로를 기다리는 장면이 아예 생기지 않아요. 그래서 락을 걸면 Lost Update 도, 이 데드락도 함께 사라집니다.

---

# 10. 정리 — 체크리스트와 용어집

### ✅ 체크리스트

- [ ] 프로세스(지점)와 스레드(창구 직원)의 차이를 말할 수 있다
- [ ] 로그의 `http-nio-8080-exec-N` 이 요청을 처리한 톰캣 스레드라는 것을 안다
- [ ] 지역 변수(스택)는 안전하고, 싱글톤 Bean 의 필드(힙)는 위험하다는 것을 안다
- [ ] 읽기-계산-쓰기 시간표로 Lost Update 를 그릴 수 있다
- [ ] `synchronized` 와 `AtomicLong` 으로 메모리 안의 값을 지킬 수 있다
- [ ] `synchronized` 로 DB 의 잔액을 못 지키는 이유 두 가지(commit 틈, 서버 여러 대)를 말할 수 있다
- [ ] `ExecutorService` · `CountDownLatch` 로 동시 실행 테스트를 읽고 쓸 수 있다
- [ ] MDC · `@Transactional` 이 ThreadLocal 이라 스레드를 넘어가지 않는다는 것을 안다

### 📚 용어집

| 용어 | 쉽게 말하면 |
|---|---|
| 프로세스 | 실행 중인 프로그램. 은행 지점 하나 |
| 스레드 | 프로그램 안의 일꾼. 창구 직원 |
| 멀티스레드 | 직원 여러 명이 동시에 일하는 것 |
| 스택 / 힙 | 직원 개인 메모장 / 지점 공용 서류함 |
| 싱글톤 Bean | 서버 전체에 하나만 있고 모든 스레드가 같이 쓰는 객체 |
| 무상태(Stateless) | 요청별 값을 필드에 저장하지 않는 설계 |
| 경쟁 상태 | 실행 순서에 따라 결과가 달라지는 상황 |
| 임계 영역 | 한 번에 한 스레드만 들어가야 하는 코드 구역 |
| `synchronized` | JVM 안에서 쓰는 "열쇠 하나" |
| `AtomicLong` | 읽기·계산·쓰기를 한 번에 해 주는 숫자 |
| 스레드 풀 | 미리 뽑아 둔 직원들. 일감을 나눠 받는다 |
| `CountDownLatch` | 숫자가 0 이 될 때까지 기다리게 하는 카운트다운 전광판 |
| ThreadLocal | 직원마다 따로 있는 개인 사물함 |
| 커넥션 풀 | DB 출입증 묶음 |
| 교착상태 | 서로가 쥔 열쇠를 기다리며 둘 다 멈춘 상태 |
| 가상 스레드 | Java 21 의 가벼운 스레드. 적은 진짜 스레드로 많은 요청 처리 |

### 😵 자주 만나는 에러 · 증상

| 증상 | 원인 |
|---|---|
| 동시 테스트가 끝나지 않음 | `countDown()` 을 `finally` 에 안 넣어서, 예외 난 스레드가 카운트를 안 줄임 |
| 동시 테스트에서 계좌를 못 찾음 (404) | 테스트에 `@Transactional` 을 붙여 데이터가 commit 되지 않음 |
| 로그의 요청 번호가 엉뚱한 값 | (MDC 를 직접 다룰 때) `MDC.clear()` 를 안 해서 이전 요청의 번호가 남음. 추적 라이브러리를 쓰면 자동으로 비워 준다 |
| 로그의 요청 번호 칸이 비어 있다 | 요청을 거치지 않고 직접 만든 스레드(`main`, `pool-1-thread-N` 등)라 추적 번호가 없음. 정상 |
| `synchronized` 를 붙였는데도 잔액이 틀림 | 락이 commit 전에 풀림 / 서버 여러 대 → DB 락 필요 |
| 다른 API 까지 갑자기 느려짐 | 락을 기다리는 스레드가 DB 커넥션을 쥐고 있어 커넥션 풀이 바닥남 |
| `Deadlock found when trying to get lock` | 두 이체가 서로 반대 순서로 계좌를 잠금. 또는 락 없이 한 계좌에 동시 출금할 때 거래 INSERT 의 FK 공유 락과 잔액 UPDATE 의 독점 락이 맞물림 (9절) |

---

# 🎤 면접에서는 이렇게 묻습니다

> 답변은 **"한 문장 정의 → 우리 프로젝트에서는 → 왜"** 순서로 30초 안에. 외운 티보다 **직접 해 본 경험**이 점수가 높습니다.

**Q1. 프로세스와 스레드의 차이는?**
<details><summary>모범 답안</summary>

> 프로세스는 실행 중인 프로그램이고 자기만의 메모리 공간을 가집니다. 스레드는 프로세스 안에서 코드를 실행하는 단위로, 같은 프로세스의 스레드끼리는 힙 메모리를 공유하고 스택만 따로 가집니다. 그래서 스레드끼리는 데이터를 주고받기 쉽지만, 같은 데이터를 동시에 바꾸면 동시성 문제가 생깁니다.

</details>

**Q2. Spring Boot 는 동시에 들어온 요청을 어떻게 처리하나요?**
<details><summary>모범 답안</summary>

> 내장 톰캣이 스레드 풀을 가지고 있다가, 요청마다 스레드 하나를 배정해 Controller 부터 응답까지 처리합니다. 기본 최대 스레드는 200개입니다. 저는 콘솔 로그에 찍히는 스레드 이름(`nio-8080-exec-N`)이 요청마다 배정되는 것을 확인했습니다.

</details>

**Q3. Spring Bean 은 싱글톤인데 스레드에 안전한가요?**
<details><summary>모범 답안</summary>

> Bean 자체가 안전한 건 아니고, 상태를 갖지 않게 만들어야 안전합니다. 메서드의 지역 변수는 스레드마다 스택에 따로 생기니 안전하지만, 필드에 요청별 값을 저장하면 모든 스레드가 같은 필드를 덮어써서 다른 사용자의 값이 섞입니다. 그래서 Service 는 주입받은 final 필드만 두는 무상태로 설계했습니다.

</details>

**Q4. 동시성 문제를 `synchronized` 로 해결하면 안 되나요?**
<details><summary>모범 답안</summary>

> 메모리 안의 값이라면 해결되지만, 잔액처럼 DB 에 있는 값에는 부족합니다. 첫째, `@Transactional` 과 함께 쓰면 메서드가 끝나 락이 풀린 뒤에 commit 이 일어나서 그 틈에 다른 스레드가 이전 값을 읽을 수 있습니다. 둘째, `synchronized` 는 JVM 하나 안에서만 동작해서 서버가 여러 대면 막지 못합니다. 그래서 저는 DB 의 `SELECT ... FOR UPDATE` 비관적 락으로 해결했습니다.

</details>

**Q5. 경쟁 상태(Race Condition)란? 직접 재현해 본 적 있나요?**
<details><summary>모범 답안</summary>

> 여러 스레드가 같은 값을 읽고-계산하고-쓰는 사이에 서로 끼어들어 실행 순서에 따라 결과가 달라지는 상황입니다. 저는 순수 자바로 `ExecutorService` 스레드 32개와 `CountDownLatch` 를 써서 공용 변수에서 100번 출금하면 결과가 매번 달라지고 0원이 되지 않는 것을 확인했습니다. 은행 서버에서는 같은 일이 DB 잔액에서 일어나 Lost Update 가 되기 때문에, 계좌를 읽는 순간부터 잠그는 `SELECT ... FOR UPDATE` 비관적 락으로 막았습니다.

</details>

**Q6. ThreadLocal 은 무엇이고 어디에 써 봤나요? 주의할 점은?**
<details><summary>모범 답안</summary>

> 스레드마다 따로 값을 가지는 변수입니다. 요청 추적 번호(traceId)를 MDC 에 넣어 로그에 찍을 때 썼고(추적 라이브러리가 넣고 비웁니다), Spring 의 `@Transactional` 도 내부적으로 ThreadLocal 로 트랜잭션을 스레드에 묶습니다. 톰캣은 스레드를 재사용하기 때문에 요청이 끝나면 반드시 값을 지워야 하고, 새 스레드나 `@Async` 로 넘어가면 값이 전달되지 않는다는 점을 주의해야 합니다.

</details>

**Q7. 데드락은 왜 생기고 어떻게 막나요?**
<details><summary>모범 답안</summary>

> 두 트랜잭션이 서로가 가진 락을 기다리며 둘 다 진행하지 못하는 상태입니다. 이체에서 A→B 와 B→A 가 동시에 오면, 각자 자기 출금 계좌를 먼저 잠그고 상대 계좌를 기다리게 됩니다. 저는 두 계좌를 항상 계좌번호 순서로 잠그도록 해서 잠그는 순서가 엇갈리지 않게 했습니다. 계좌 하나에도 락 없이 동시에 쓰면 MySQL 에서는 거래 INSERT 의 외래키 공유 락과 잔액 UPDATE 의 독점 락이 맞물려 데드락이 날 수 있는데, `SELECT ... FOR UPDATE` 로 독점 락을 먼저 잡게 하면 이를 피할 수 있다고 알고 있습니다.

</details>
