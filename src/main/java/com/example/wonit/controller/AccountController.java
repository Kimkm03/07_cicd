package com.example.wonit.controller;

import com.example.wonit.dto.AccountCreateRequest;
import com.example.wonit.dto.AccountPatchRequest;
import com.example.wonit.dto.AccountResponse;
import com.example.wonit.dto.TransactionResponse;
import com.example.wonit.exception.AccountNotFoundException;
import com.example.wonit.model.Account;
import com.example.wonit.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.example.wonit.dto.DepositRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

// RestController = 인터넷 주소(URL)를 받아 Service 를 부르는 부분. 돌려주는 자바 객체는 Spring 이 JSON 으로 바꿔 응답한다
// REST 규칙: URL 은 명사(무엇을), HTTP 메서드는 동사(어떻게)
//   GET    /api/accounts                     목록      200
//   GET    /api/accounts/{accountNo}         하나      200
//   POST   /api/accounts                     개설      201 + Location 헤더로 개설된 주소 전송
//   PUT    /api/accounts/{accountNo}         통째로 교체 200
//   PATCH  /api/accounts/{accountNo}/status  상태 변경 200
//   DELETE /api/accounts/{accountNo}         해지      204
//   POST   /api/accounts/{accountNo}/withdrawals    출금      201
//   POST   /api/accounts/{accountNo}/deposits       입금      201
//   GET    /api/accounts/{accountNo}/transactions   거래내역  200 (쪽 단위로 출력)
@Slf4j
@Tag(name = "swagger 테스트 API", description = "swagger 테스트를 진행하는 API")
@RestController
@RequestMapping("/api/accounts") // localhost:8080/api/accounts
public class AccountController {

    // Inversion Of Control (개발자가 Bean으로 등록해놓은 객체는 스프링 컨테이너가 관리하므로 개발자는 가져다 쓰기만 하면 됩니다.)ㄴ
    // AccountService aService = new AccountService();
    // 어차피 스프링컨테이너가 관리하는 bean이므로 private으로 막아둡니다.
    @Autowired // 서로서로 연결된 bean임을 알려줍니다. 생략하면 Springboot가 넣어줌
    private AccountService accountService;

    // 모든 accounts를 조회
    // jpa의 규칙은 Repository에서만 영향을 미칩니다.
    @Operation(summary = "정보 모두 조회", description = "Account의 전체 정보를 조회합니다.")
    @GetMapping
    public List<AccountResponse> findAll(@AuthenticationPrincipal Jwt jwt) {   // 🔐 토큰 주인의 계좌만
    //public List<Account> findAll() {
        // stream().filter() : 값이 true인 것만 반환
        // stream().map() : 모든 값에 같은 메서드를 일괄 적용
        log.info("전체 계좌 조회 시작");
        return accountService.findMine(Integer.valueOf(jwt.getSubject())).stream().map(AccountResponse::from).toList();
    }

    // 🔐 주소의 계좌번호가 토큰 주인의 계좌인지 먼저 확인한다 (남의 계좌면 403).
    //    계좌번호가 주소에 들어가는 API(조회 · 수정 · 해지 · 거래내역)는 모두 맨 앞에서 부른다
    private void checkMine(Jwt jwt, String accountNo) {
        accountService.checkMine(Integer.valueOf(jwt.getSubject()), accountNo);
    }

//    // @RequestBody : 숨기고 싶은 정보, 주소줄에 전달하기에 너무 긴 값들을 Body에 싣고 전달하는 방법
//    // ResponseEntity : 응답코드를 구체적으로 넘겨주기 위한 스프링부트의 Wrapper 클래서
//    @PostMapping
//    public ResponseEntity<AccountResponse> createAccount(
//            @RequestBody AccountCreateRequest request
//    ) {
//        Account account = accountService.createAccount(request);
//
//        // 새로 발급받은 {accountNo}로 주소를 돌려서 호출하는 부분
//        URI location = ServletUriComponentsBuilder
//                .fromCurrentContextPath()
//                .path("/api/accounts/{accountNo}")
//                .buildAndExpand(account.getAccountNo())
//                .toUri();
//
//        return ResponseEntity
//                .created(location)
//                .body(AccountResponse.from(account));
//    }

    // @RequestBody : 숨기고 싶은 정보, 주소줄에 전달하기에 너무 긴 값들을 Body에 싣고 전달하는 방법
    /**
     * 새 계좌를 개설합니다.
     *
     * <p>요청 예시:
     * <pre>
     * POST /api/accounts
     * Content-Type: application/json
     * {"customerId": 1, "accountType": "적금"}
     * </pre>
     *
     * <p>계좌번호, 초기 잔액, 상태는 서버에서 설정합니다. 계좌 ID는
     * {@code Account}의 IDENTITY 전략으로 DB가 생성합니다.
     * H2에서 시드 데이터가 {@code account_id} 1, 2, 3을 사용한다면,
     * 시드 INSERT 이후 다음 ID가 4가 되도록 설정해야 기본 키 충돌을 방지할 수 있습니다.
     *
     * @param request 개설할 고객 ID와 계좌 종류
     * @return 생성된 계좌 정보
     */
    @PostMapping
    public AccountResponse createAccount(@AuthenticationPrincipal Jwt jwt, @RequestBody AccountCreateRequest request) {
        // 🔐 누구의 계좌인지는 토큰이 정한다 (Body 의 customerId 는 쓰지 않는다 → 남의 이름으로 개설 불가)
        request = new AccountCreateRequest(Integer.valueOf(jwt.getSubject()), request.accountType());
        log.debug("계좌 개설 요청: customerId={}, accountType={}",
                request.customerId(), request.accountType());

        Account account = accountService.createAccount(request);

        log.debug("계좌 개설 응답: accountId={}, accountType={}",
                account.getAccountId(), account.getAccountType());
        return AccountResponse.from(account);
    }

    // 특정 계좌번호의 세부 정보를 조회  localhost:8080/api/accounts/1002-345-678901
    /**
     * 계좌번호로 계좌 하나를 찾는다.
     * @param accountNo 찾을 계좌번호 (예: 1002-345-112233)
     * @return 찾은 계좌
     * @throws AccountNotFoundException 그런 계좌가 없을 때
     */
    @GetMapping("/{accountNo}")
    public AccountResponse findOne(@AuthenticationPrincipal Jwt jwt, @PathVariable("accountNo") String accountNo){
        checkMine(jwt, accountNo);
    // public Account findOne(@PathVariable("accountNo") String accountNo){
        // log.trace("trace level의 로그 - 자잘함");
        // log.debug("debug level의 로그 - 개발자가 flow 따라갈 수 있는 수준의 로그");
        log.info("info level의 로그 - 사용자나 시스템의 의미있는 내용을 담은 로그, 실제 운영환경에는 info 이상만 수집");
        // log.warn("warn level의 로그 - 현재는 문제 없지만 앞으로 문제가 될 수 있는 상황에 대한 경고");
        // log.error("error level의 로그 - 치명적인 로그");
        // log.info("계좌 조회=" + accountNo); // 먼저 문자열을 더하는 과정은 거치고 나서 로그를 제외
        log.info("계좌 조회 시작={}", accountNo); // 아예 보지도 않고 실행
        return AccountResponse.from(accountService.getAccount(accountNo));
    }

    // 계좌 전체를 조회해서 그중에 '정상' 계좌만 계좌번호만 출력
    @GetMapping("/report1")
    public List<String> normalAccountNos() {
        return accountService.normalAccountNos();
    }

    // 전체 계좌중에 account_type이 '적금'인 경우만 금액을 모두 더해서 리턴
    // /api/accounts/report2 라는 주소로
    @GetMapping("/report2")
    // public long savingBalanceSum() {
    public BigDecimal savingBalanceSum() {
        return accountService.savingBalanceSum();
    }

    // min~ max balance 사이의 계좌 정보를 검색
    // http://localhost:8080/api/accounts/balance?minBalance=500000&maxBalance=1300000
    // 동작은 정상적으로 하겠지만 빈 배열이 출력됩니다

    /**
     * 지정한 최소 잔액과 최대 잔액 사이의 계좌를 조회합니다.
     *
     * <p>요청 예시:
     * <pre>
     * GET /api/accounts/balance?minBalance=500000&amp;maxBalance=1300000
     * </pre>
     *
     * <p>경계값을 포함하여 조회합니다. Service에 전달할 때도 반드시
     * {@code minBalance}를 먼저, {@code maxBalance}를 나중에 전달해야 합니다.
     * 순서를 바꾸면 검색 조건이 뒤집혀 결과가 비어 있을 수 있습니다.
     *
     * @param minBalance 조회할 최소 잔액
     * @param maxBalance 조회할 최대 잔액
     * @return 잔액이 지정 범위에 포함되는 계좌 목록
     */
    @GetMapping("/balance")
    public List<Account> getAccountBetweenBalance(@RequestParam long minBalance, @RequestParam long maxBalance) {
        return accountService.getAccountBetweenBalance(minBalance, maxBalance); // 파라미터 순서 바꿈
    }

    // balance 순서대로 작은 금액에서 큰 금액 순서로 모든 계좌 조회
    @GetMapping("/sort") // localhost:8080/api/accounts/sort
    public List<AccountResponse> findAllAccountsByBalanceAsc(){
        return accountService.findAllAccountsByBalanceAsc().stream().map(AccountResponse::from).toList();
    }

    // status 에서 정상 / 휴면 / 지급정지 들어오면 각 상황에 맞는 계좌만 필터링
    // localhost:8080/api/accounts/status=정상
    @GetMapping("/status/{status}") // localhost:8080/api/accounts/accounts/정상 -> 같은 방식으로 값을 입력받는 함수가 2개 있음
    public List<AccountResponse> findAllAccountsByStatus(@PathVariable String status){
        return accountService.findAllAccountsByStatus(status).stream().map(AccountResponse::from).toList();
    }

    // account_type에 적금 / 입출금 들어오면 각 상황에 맞는 계좌만 필터링
    // 1. accountType (camelCase: 자바스타일)
    // 2. account_type (snake_case: DB스타일)
    // 3. account-type (kebab-case: HTTP스타일) - REST API의 권장사항
    @GetMapping("/account-type/{accountType}") // localhost:8080/api/accounts/account-type/입출금 -> 같은 방식으로 값을 입력받는 함수가 2개 있음
    public List<AccountResponse> findAllAccountsByAccountType(@PathVariable String accountType){
        return accountService.findAllAccountsByAccountType(accountType).stream().map(AccountResponse::from).toList();
    }

    // 일반 JPA의 규칙대로
    //   PUT    /api/accounts/{accountNo}         통째로 교체 200
    // PUT 은 "이 주소의 자원을 내가 보낸 내용으로 통째로 바꿔 주세요"
    //   - 고칠 수 있는 칸을 전부 보내야 한다. 하나라도 빠지면 400
    //   - 같은 요청을 몇 번 보내도 결과가 같다 (멱등). 두 번 보내도 accountType · status 는 똑같다
    // PATCH 와 비교: PATCH /status 는 status 한 칸만 보내고, 나머지 칸은 그대로 둔다
    @PutMapping("/{accountNo}")
    public AccountResponse updateOne(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable("accountNo") String accountNo, // 주소줄
                                     @RequestBody AccountCreateRequest request) { // Body
        checkMine(jwt, accountNo);
        // 🔐 주인(customerId)은 토큰이 정한다. Body 로 주인을 바꿔 남에게 넘기거나 빼앗을 수 없다
        request = new AccountCreateRequest(Integer.valueOf(jwt.getSubject()), request.accountType());
        return AccountResponse.from(accountService.updateOne(accountNo, request));
    }

    // 10.19 월요일 수업
    // 일반 JPA의 규칙대로
    //   PATCH  /api/accounts/{accountNo}/status  상태 변경 200
    // 상태 변경 (PATCH): 변경 감지가 없으므로 save() 를 직접 부른다
    // findOne() 안의 findById() 는 자기 트랜잭션을 열고 조회가 끝나면 바로 닫는다
    // → 엔티티 값을 바꿔도 자바 객체만 바뀌고, 끝날 때 비교해 줄 트랜잭션이 없다
    @Operation(summary = "계좌 상태 변경", description = "정상 · 지급정지 · 휴면 중 하나로 바꿉니다. 해지는 DELETE 로 합니다.")
    @PatchMapping("/{accountNo}/status")
    public AccountResponse changeStatus(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable("accountNo") String accountNo,
                                        @Valid @RequestBody AccountPatchRequest request) {
        checkMine(jwt, accountNo);
        return AccountResponse.from(accountService.patchAccount(accountNo, request));
    }


    // 입금 (내 계좌에만). React 가 보내는 Idempotency-Key 헤더는 읽지 않는다 (무시) — 막는 원리는 Day 1 멱등성에서 다룬다 (이 수업에서는 코드로 만들지 않는다)
    @PostMapping("/{accountNo}/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable("accountNo") String accountNo,
                                       @Valid @RequestBody DepositRequest request) {
        checkMine(jwt, accountNo);
        return TransactionResponse.from(accountService.deposit(accountNo, request));
    }

    //   DELETE /api/accounts/{accountNo}         해지      204
    @DeleteMapping ("/{accountNo}")
    public void deleteOne(@AuthenticationPrincipal Jwt jwt, @PathVariable("accountNo") String accountNo) {
        checkMine(jwt, accountNo);
        accountService.deleteOneByAccountNo(accountNo);
//        accountService.deleteOne(accountNo);
    }

    // GET /api/accounts/1002-345-678901/transactions?page=0&size=20
    // ?page=0&size=20 을 Spring 이 Pageable 로 바꿔 준다. page 는 0부터 시작, 기본은 최신순 20건
    // 응답은 배열이 아니라 { "content": [...], "page": { "size", "number", "totalElements", "totalPages" } } 모양이다 (Page → common/WebConfig 의 VIA_DTO)
// Controller — ?page=0&size=20 을 Spring 이 Pageable 로 바꿔 준다
    @GetMapping("/{accountNo}/transactions")
    public Page<TransactionResponse> transactions(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable("accountNo") String accountNo, Pageable pageable) {
        checkMine(jwt, accountNo);
        // 정렬은 서버가 최신순으로 고정한다
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "txDatetime"));
        return accountService.getTransactions(accountNo, newestFirst).map(TransactionResponse::from);
    }

}
