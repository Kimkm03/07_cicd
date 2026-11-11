# 우리WON 뱅킹 (05_springAI)

Spring Boot 로 만든 작은 은행 API 와 React 화면으로 **멱등성 · Spring AI · 회원가입 · JWT · 요청 추적 · Actuator · CORS · React 연결**을 익히고, 이어서 **Docker · AWS 배포(HTTPS) · CI/CD · 모니터링 · 부하 테스트** 까지 해 보는 수업용 레포입니다.

- 백엔드: Spring Boot 4.1 · Java 21 · JPA · H2 (메모리 DB, MySQL 프로필 선택), 루트 폴더
- 프론트엔드: React 19 · Vite 8, `frontend/` 폴더
- 시작점은 10/15(목) 이후 수업에서 함께 만든 코드(`wonbanking-backend` main, `511bbcb`)이고, 이 코드가 첫 커밋 **`t0-start`**(Day 0)입니다. 그 위에 **주제별 태그 11개(`t1` ~ `t10-monitoring`, 커밋 12개)** 를 쌓았습니다.

## 실행하기

프로필도 환경변수도 필요 없습니다. H2 메모리 DB 와 데모 데이터가 기본입니다.

```bash
# 터미널 1: 서버 (http://localhost:8080)
./gradlew bootRun

# 터미널 2: 화면 (http://localhost:5173)
cd frontend
npm ci
npm run dev
```

> 화면 주소는 반드시 `http://localhost:5173` 입니다. 서버의 CORS 설정이 이 주소만 허락합니다.

### 데모 로그인

| 아이디 | 비밀번호 | 이름 | 계좌 |
|---|---|---|---|
| `yeonji` | `1234` | 김연지 | `1002-345-678901` 지급정지 · `1002-345-112233` 정상 · `1002-345-998877` 휴면 |
| `woori` | `1234` | 이우리 | `1002-555-667788` 정상 |

새 아이디는 화면의 **회원가입** 이나 `POST /api/auth/signup` 으로 만듭니다. 가입하면 0원 입출금 계좌가 하나 자동으로 열립니다. (비밀번호는 4자 이상, 같은 아이디는 409)

### MySQL 로 실행 (선택)

기본은 H2 메모리 DB 라서 서버를 다시 켜면 데이터가 사라집니다. MySQL(예: AWS RDS)에 남기려면 `mysql` 프로필로 켭니다. 주소 · 계정 · 비밀번호는 환경변수로만 넣고, `bankdb` 데이터베이스는 미리 만들어 둡니다.

```bash
DB_URL='jdbc:mysql://<엔드포인트>:3306/bankdb?serverTimezone=Asia/Seoul&characterEncoding=UTF-8' DB_USERNAME=... DB_PASSWORD=... JWT_SECRET=... ./gradlew bootRun --args='--spring.profiles.active=mysql'
```

- IntelliJ 는 실행 설정의 Active profiles 에 `mysql`, Environment variables 에 `DB_URL=...;DB_USERNAME=...;DB_PASSWORD=...;JWT_SECRET=...`
- 🚨 **데모 데이터가 없습니다.** `data.sql` 은 H2 용이라 이 프로필에서는 실행하지 않습니다. `yeonji` 로는 로그인되지 않으니, 먼저 회원가입을 합니다.

### 확인용 주소

| 무엇 | 주소 |
|---|---|
| H2 콘솔 | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:testdb`, 사용자 `sa`, 비밀번호 비움) |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| 서버 상태 | http://localhost:8080/actuator/health |

요청 예제는 IntelliJ 의 `src/api.http` 에 모여 있습니다. Postman 을 쓰면 `docs/postman/wonbanking.postman_collection.json` 을 Import 하세요 (Day 0 ~ Day 3 의 요청 58개, 사용법은 Day 0 자료의 "Postman 으로 전체 API 호출하기"). 서버를 다시 시작하면 데이터는 처음 상태로 돌아갑니다.

## 과정과 태그

각 태그가 그날의 **완성본**입니다. 따라가다 막히면 `git switch -c my-t9 t9-cicd` 처럼 태그에서 내 브랜치를 만들어 이어가세요.

| 날 | 태그 | 주제 | 자료 |
|---|---|---|---|
| Day 0 | `t0-start` | PUT · PATCH 차이, DELETE(해지 · Soft Delete), 페이징, Header · Body, BigDecimal, `@Transactional`, 비관적 락, JUnit 테스트 (코드 추가 없음, 읽고 실행) | [day0-update-money-tx-lock.md](docs/day0-update-money-tx-lock.md) |
| Day 1 | `t1-idempotency` | 같은 요청을 두 번 보내 보기 (코드 추가 없음, `api.http`) | [day1-idempotency-ai.md](docs/day1-idempotency-ai.md) |
| Day 1 | `t2-ai` | Spring AI 로 거래 메모를 카테고리로 분류, 제공사 전환, AI 저축 도우미(Tool Calling · 프롬프트 파일 · 대화 기억) | [day1-idempotency-ai.md](docs/day1-idempotency-ai.md) |
| Day 2 | `t3-jwt` | 쿠키 · 세션과 JWT, 회원가입(BCrypt) · 로그인 · 토큰 인증 · 내 계좌만, MySQL 프로필(안내) | [day2-jwt-tracing-actuator.md](docs/day2-jwt-tracing-actuator.md) |
| Day 2 | `t4-tracing` | 요청 추적 (traceId · spanId, `X-Request-Id`) | [day2-jwt-tracing-actuator.md](docs/day2-jwt-tracing-actuator.md) |
| Day 2 | `t5-actuator` | Actuator (health · info · metrics) | [day2-jwt-tracing-actuator.md](docs/day2-jwt-tracing-actuator.md) |
| Day 3 | `t6-rag` (선택) | RAG: 은행 안내문 검색을 AI 저축 도우미(`/api/ai/chat`)의 도구로 | [day3-rag-optional.md](docs/day3-rag-optional.md) |
| Day 3 | `t6-react` | 입금 · 이체 API, CORS, React 연결(회원가입 · 로그인 화면), AI 저축 도우미 채팅창 | [day3-react.md](docs/day3-react.md) |
| Day 4 | `t7-docker` | Docker 기본 · 리눅스 명령, Dockerfile 직접 쓰기(한 단계 `Dockerfile-single` → 멀티 스테이지 · alpine 으로 이미지 줄이기), Docker Hub | 수업 진행 |
| Day 5 | `t8-prod` | AWS ①: CloudFormation 스택 2개(버지니아 인증서 · 서울 전체)로 VPC · NAT · Bastion · RDS · ALB · Auto Scaling · S3 · CloudFront · 모니터링 EC2 를 한 번에, prod 설정, GitHub Actions 서버 CI/CD(테스트 → 이미지 → `:prod` → 인스턴스 새로 고침, OIDC) | [day5-aws.md](docs/day5-aws.md) |
| Day 6 | `t9-cicd` | AWS ②: 스택이 만든 S3 · CloudFront 보기, 화면 자동 배포(빌드 → S3 → 무효화, OIDC) | [day6-cicd-monitoring.md](docs/day6-cicd-monitoring.md) |
| Day 6 | `t10-monitoring` | Prometheus 지표(`build.gradle` 1줄), 스택이 띄운 Prometheus(ASG 서버 자동 찾기) + Grafana(+CloudWatch) 연결 | [day6-cicd-monitoring.md](docs/day6-cicd-monitoring.md) |
| Day 7 | `t10-monitoring` (코드 그대로) | k6 부하 테스트, Auto Scaling 자동 확장(대상 추적), 스택 업데이트로 값 바꾸기, 알림 설계 · 스택 삭제로 정리 | [day7-load-scale.md](docs/day7-load-scale.md) |

스레드와 동시성 보충자료(선택)는 [docs/reference/threads.md](docs/reference/threads.md), LAZY · EAGER · N+1 · 순환 참조 참고자료는 [docs/reference/fetch-strategy.md](docs/reference/fetch-strategy.md) 입니다.

## 날마다 시작하기

레포는 처음에 한 번만 받고, 날마다 **그날 시작 태그에서 내 브랜치**를 만들어 실행합니다. 수업자료는 GitHub 의 **main** 에서 읽습니다 (Day 0 자료는 main 에만 있습니다).

### 준비 (수업 전 한 번)

```bash
git clone https://github.com/WONIT-A/05_springAI.git
cd 05_springAI
```

- 필요한 것: JDK 21, IntelliJ, Node.js (Day 3 화면용), Docker Desktop (Day 4 부터), AWS 계정 (Day 5 부터, 수업 때 받는 IAM 사용자)
- AI 키는 Day 1 전에 미리 발급해 둡니다. 키가 없어도 앱은 뜨고, AI 부분만 안내 문구가 나옵니다.

### 날마다

```bash
git switch -c my-day0 t0-start          # 그날 시작 태그에서 내 브랜치 만들기 (Day 1 이면 t1-idempotency)
./gradlew bootRun                       # 서버 실행 (H2 메모리 DB, 시드 데이터 자동)
```

- 태그를 바꾼 뒤에는 IntelliJ 에서 **Gradle 새로고침**(코끼리 아이콘)을 합니다. `t2` 에서 AI · `t3` 에서 보안 · `t10` 에서 Prometheus 의존성이 추가됩니다.
- 수업 중 다음 단계로 넘어갈 때는 내 수정을 커밋한 뒤 `git switch -c my-t9 t9-cicd` 처럼 다음 태그로 갈아탑니다. **뒤처졌을 때도 같은 방법으로 바로 따라잡습니다.**

| 날 | 시작 태그 → 끝 태그 | 실행 | 요청 보내기 |
|---|---|---|---|
| Day 0 | `t0-start` (10/15(목) 이후 수업 코드 그대로) | `./gradlew bootRun` | Swagger 또는 Postman 컬렉션 (`src/api.http` 는 `t1` 부터). 토큰 없음 |
| Day 1 | `t1-idempotency` → `t2-ai` | 키 없이: `./gradlew bootRun`<br>키 있으면: IntelliJ 실행 설정의 Active profiles 에 `gemini`, 환경변수에 `GEMINI_API_KEY=...;AI_MODEL=...` | `api.http` 또는 Swagger. 아직 로그인이 없어서 토큰 없이 된다 (데모 고객 1번) |
| Day 2 | `t3-jwt` → `t4-tracing` → `t5-actuator` | `./gradlew bootRun` | **`api.http` 맨 위의 로그인 요청을 먼저 실행** → 토큰이 저장되고 아래 요청들이 그 토큰을 쓴다. 새 아이디는 `api.http` 의 회원가입 요청으로 만든다.<br>Swagger 로 하려면 `POST /api/auth/login` 을 먼저 실행하고, 오른쪽 위 **Authorize** 에 `accessToken` 을 붙여 넣는다 |
| Day 3 | `t5-actuator` → (`t6-rag` 선택) → `t6-react` | 터미널 1: `./gradlew bootRun`<br>터미널 2: `cd frontend && npm ci && npm run dev` | 브라우저에서 **http://localhost:5173** 접속, `yeonji` / `1234` 로그인 (또는 **회원가입**) |
| Day 4 | `t6-react` → `t7-docker` | `docker build -t <도커허브ID>/wonbank .` → `docker run --rm -p 8080:8080 --env-file .env <도커허브ID>/wonbank` | Swagger · Postman (`localhost:8080`, 로그인 먼저). 화면은 Day 3 처럼 `npm run dev` |
| Day 5 | `t7-docker` → `t8-prod` | 도메인 · `infra/wonit-a-cert.yaml`(버지니아) → 내 저장소에 push(Actions 가 `:prod` 이미지) → `infra/wonit-a-aws.yaml`(서울, 전부) → 스택 출력을 GitHub 변수에 → push 하면 서버 교체 | `https://<이름>.woncare.shop/api/…` 로그인 |
| Day 6 | `t8-prod` → `t9-cicd` → `t10-monitoring` | 스택 출력을 GitHub 변수에 → push 하면 Actions 가 화면 배포 → 지표 코드 push → Grafana 연결 | `https://<이름>.woncare.shop`, Grafana `http://<모니터링 EC2 IP>:3000` |
| Day 7 | `t10-monitoring` | 모니터링 EC2 에서 k6 → ALB (`docker run … grafana/k6`) | Grafana · Auto Scaling 그룹 활동 |

- AI 키는 **파일에 적지 않고 환경변수로만** 넣습니다. `t6-rag` 부터는 `application.yml` 이 Gemini 를 기본으로 써서 **프로필 없이 `GEMINI_API_KEY` 만** 넣으면 됩니다 (채팅 모델 기본값 `gemini-3.5-flash-lite`, RAG 임베딩은 Gemini 전용 연결). `t2` ~ `t5` 는 `application-gemini.yml` 이 `${GEMINI_API_KEY}` 를 읽습니다. OpenRouter 는 프로필 `openrouter` + `OPENROUTER_API_KEY`, OpenAI 는 `openai` + `OPENAI_API_KEY`. `.env` 파일은 Spring Boot 가 자동으로 읽지 않습니다.
- Git Bash 로 키와 함께 실행하기: `t6-rag` 부터 `GEMINI_API_KEY=... ./gradlew bootRun`, `t2` ~ `t5` 는 `GEMINI_API_KEY=... AI_MODEL=... ./gradlew bootRun --args='--spring.profiles.active=gemini'`

### 자주 막히는 곳

| 증상 | 원인과 해결 |
|---|---|
| 태그를 바꿨더니 빨간 줄 · import 오류 | Gradle 새로고침을 안 했다 |
| `t3` 부터 모든 요청이 401 | `api.http` 의 로그인 요청을 먼저 실행하지 않았다. Swagger 는 오른쪽 위 **Authorize** 에 `accessToken` 을 붙여 넣었는지 확인한다. 토큰이 만료되면(`t3` ~ `t5` 30분, `t6-rag` 부터 300분) 다시 로그인한다 |
| `mysql` 프로필로 켰더니 `yeonji` 로그인이 401 | 정상. 이 프로필에는 데모 데이터가 없다. 회원가입으로 새 아이디를 만든다 |
| 남의 계좌 주소로 요청하면 403 | 정상. `t3` 부터 주소의 계좌번호가 토큰 주인의 계좌인지 확인한다 (`woori` 로 `yeonji` 계좌를 부르면 403, 없는 계좌는 404) |
| 서버를 다시 켰더니 데이터가 원래대로 | 정상. 메모리 DB 라서 켤 때마다 시드 데이터로 시작한다 |
| `Port 8080 already in use` | 다른 창에서 서버가 이미 실행 중. 그 서버를 끈다 |
| AI 답이 늘 `기타` 나 "쓸 수 없습니다" | 키 · 모델 이름 · 프로필 중 하나가 빠졌다. 콘솔의 WARN 로그에 원인이 나온다 |
| 화면이 서버를 못 부른다 (CORS) | 화면은 반드시 `localhost:5173` 으로 연다. 다른 포트 · IP 는 막힌다 |
| Mac · Linux 에서 `./gradlew` 권한 오류 | `chmod +x gradlew` 한 번 (Windows Git Bash 는 그대로 된다) |
| `docker run` 했는데 컨테이너가 바로 꺼진다 | `--env-file` 의 `.env` 가 없거나 값이 비었다 (`GEMINI_API_KEY=` 처럼 빈 값도 안 된다 → 줄째 지운다). `docker logs <이름>` 으로 원인을 본다 |
| 앱 서버(EC2)에서 `exec format error` | Apple 칩 Mac 에서 만든 이미지. `docker buildx build --platform linux/amd64 -t <id>/wonbank:t8 --push .` 로 다시 만든다 |
| 내 수정이 있는데 태그를 못 바꾼다 | `git add . && git commit -m "내 실습"` 으로 저장한 뒤 바꾼다 |

## 테스트

```bash
./gradlew test
```

리포트 · AI 도우미는 `ReportFlowTest`, 회원가입 · 로그인 · 내 계좌만 · 남의 계좌 403 은 `AuthFlowTest`, 입금 · 이체 흐름은 `TransferFlowTest` 가 확인합니다. Day 0 의 테스트(`AccountTest` 등)는 Day 0 자료 8절에서 다룹니다.

태그별 전체 테스트 수는 `t0` 16 · `t1` 16 · `t2` 22 · `t3` 31 · `t4` 31 · `t5` 31 · `t6-rag` 33 · `t6-react` 40 · `t7` 40 · `t8` 40 · `t9` 40 · `t10` 40 개입니다. 태그마다 1개(`TestLifeCycle.test3`, `@Disabled`)는 건너뜁니다.

## 일부러 넣지 않은 것

- `Idempotency-Key` 처리: React 는 헤더를 보내지만 서버는 읽지 않고 무시합니다 (같은 이체를 두 번 보내면 두 번 처리됨)
- `Transfer` 표와 수수료
- 회원 관리 기능: 비밀번호 변경 · 탈퇴 · 이메일 인증 · 토큰 취소(로그아웃 서버 처리)는 없습니다. 회원가입은 아이디 · 비밀번호 · 이름만 받습니다
