# 우리WON 뱅킹 (07_cicd)

Spring Boot 로 만든 은행 API 와 React 화면입니다. 이 레포는 **CloudFormation · GitHub Actions · Prometheus · Grafana · k6** 수업의 시작점입니다.

- 백엔드: Spring Boot 4.1 · Java 21 · JPA · H2 (메모리 DB, MySQL 프로필 선택), 루트 폴더
- 프론트엔드: React 19 · Vite 8, `frontend/` 폴더
- AI: Spring AI 로 거래 메모 분류와 AI 저축 도우미(채팅). 키가 없어도 앱은 뜨고, AI 부분만 안내 문구가 나옵니다.

## 시작하기

1. GitHub 에서 **WONIT-A/07_cicd** 를 내 계정으로 **Fork** 합니다.
2. 내 Fork 의 **Actions** 탭에서 **I understand my workflows, go ahead and enable them** 버튼을 누릅니다. (Fork 한 레포는 워크플로가 꺼진 채로 시작합니다.)
3. 내 PC 로 받습니다.

```bash
git clone https://github.com/<GitHub 아이디>/07_cicd.git
cd 07_cicd
```

4. 수업은 **[docs/aws-cloudformation-guide.md](docs/aws-cloudformation-guide.md) 하나만** 따라가면 됩니다. 인증서 스택, 서울 스택, 서버 · 화면 자동 배포, 상황판, 부하 테스트, 정리까지 순서대로 나옵니다.

필요한 것: JDK 21, IntelliJ, Node.js, Docker Desktop, AWS 계정(수업 때 받는 IAM 사용자), GitHub 계정, Docker Hub 계정

## 폴더 안내

| 위치 | 내용 |
|---|---|
| `src/` | Spring Boot 백엔드 (`src/main`, `src/test`, 요청 예제 `src/api.http`) |
| `frontend/` | React · Vite 화면 |
| `infra/` | CloudFormation 템플릿: `wonit-a-cert.yaml`(버지니아 인증서), `wonit-a-aws.yaml`(서울 전체) |
| `.github/workflows/` | `backend.yml`(서버 CI/CD), `frontend.yml`(화면 배포). 이미 켜져 있는 워크플로입니다 |
| `ci/` | 위 워크플로의 원본 사본 (참고용) |
| `load/` | k6 부하 테스트 스크립트 `transfers.js` |
| `docs/` | 수업 자료와 참고 자료 |
| `Dockerfile`, `Dockerfile-single` | 서버 이미지 (멀티 스테이지 / 한 단계) |

## 내 PC 에서 실행하기

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

Mac · Linux 에서 `./gradlew` 권한 오류가 나면 `chmod +x gradlew` 를 한 번 실행합니다.

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
- 데모 데이터가 없습니다. `data.sql` 은 H2 용이라 이 프로필에서는 실행하지 않습니다. `yeonji` 로는 로그인되지 않으니 먼저 회원가입을 합니다.

### AI 키 (선택)

AI 키는 파일에 적지 않고 **환경변수로만** 넣습니다. 기본 설정은 Gemini 이고 `GEMINI_API_KEY` 를 읽습니다. `.env` 파일은 Spring Boot 가 자동으로 읽지 않습니다.

```bash
GEMINI_API_KEY=... ./gradlew bootRun
```

채팅 모델 이름은 `AI_MODEL` 로 바꿀 수 있습니다. OpenRouter 는 프로필 `openrouter` + `OPENROUTER_API_KEY`, OpenAI 는 `openai` + `OPENAI_API_KEY` 입니다.

### 확인용 주소

| 무엇 | 주소 |
|---|---|
| H2 콘솔 | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:testdb`, 사용자 `sa`, 비밀번호 비움) |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| 서버 상태 | http://localhost:8080/actuator/health |

요청 예제는 IntelliJ 의 `src/api.http` 에 있습니다. `api.http` 맨 위의 로그인 요청을 먼저 실행하면 토큰이 저장되고, 아래 요청들이 그 토큰을 씁니다. Postman 을 쓰면 `docs/postman/wonbanking.postman_collection.json` 을 Import 하세요. Swagger 는 `POST /api/auth/login` 을 먼저 실행하고, 오른쪽 위 **Authorize** 에 `accessToken` 을 붙여 넣습니다.

## 테스트

```bash
./gradlew test
```

리포트 · AI 도우미는 `ReportFlowTest`, 회원가입 · 로그인 · 내 계좌만 · 남의 계좌 403 은 `AuthFlowTest`, 입금 · 이체 흐름은 `TransferFlowTest` 가 확인합니다. GitHub Actions 의 `backend.yml` 도 push 할 때 이 테스트를 먼저 실행합니다.

## 참고 자료

| 자료 | 내용 |
|---|---|
| [docs/aws-cloudformation-guide.md](docs/aws-cloudformation-guide.md) | 수업에서 따라가는 메인 가이드 |
| [docs/frontend-s3-cloudfront.md](docs/frontend-s3-cloudfront.md) | 화면을 S3 · CloudFront 로 배포하는 자료 |
| [docs/day0-update-money-tx-lock.md](docs/day0-update-money-tx-lock.md) ~ [docs/day7-load-scale.md](docs/day7-load-scale.md) | 앞선 수업의 일별 자료 (day0 · day1 · day2 · day3 · day5 · day6 · day7) |
| [docs/reference/threads.md](docs/reference/threads.md) | 스레드와 동시성 보충자료 |
| [docs/reference/fetch-strategy.md](docs/reference/fetch-strategy.md) | LAZY · EAGER · N+1 · 순환 참조 |

## 자주 막히는 곳

| 증상 | 원인과 해결 |
|---|---|
| 모든 요청이 401 | 로그인을 먼저 하지 않았다. 토큰이 만료되면 다시 로그인한다 |
| `mysql` 프로필로 켰더니 `yeonji` 로그인이 401 | 정상. 이 프로필에는 데모 데이터가 없다. 회원가입으로 새 아이디를 만든다 |
| 남의 계좌 주소로 요청하면 403 | 정상. 주소의 계좌번호가 토큰 주인의 계좌인지 확인한다 (없는 계좌는 404) |
| 서버를 다시 켰더니 데이터가 원래대로 | 정상. 메모리 DB 라서 켤 때마다 시드 데이터로 시작한다 |
| `Port 8080 already in use` | 다른 창에서 서버가 이미 실행 중. 그 서버를 끈다 |
| AI 답이 늘 `기타` 나 "쓸 수 없습니다" | 키 · 모델 이름 · 프로필 중 하나가 빠졌다. 콘솔의 WARN 로그에 원인이 나온다 |
| 화면이 서버를 못 부른다 (CORS) | 화면은 반드시 `localhost:5173` 으로 연다. 다른 포트 · IP 는 막힌다 |
| Actions 탭에 워크플로가 안 보이거나 돌지 않는다 | Fork 에서 **I understand my workflows, go ahead and enable them** 을 눌렀는지 확인한다 |
| 앱 서버(EC2)에서 `exec format error` | Apple 칩 Mac 에서 만든 이미지. `docker buildx build --platform linux/amd64 ... --push .` 로 다시 만든다 |

AWS 배포 중에 막힌 경우는 가이드의 "자주 막히는 곳" 을 먼저 봅니다.

## 일부러 넣지 않은 것

- `Idempotency-Key` 처리: React 는 헤더를 보내지만 서버는 읽지 않고 무시합니다 (같은 이체를 두 번 보내면 두 번 처리됨)
- `Transfer` 표와 수수료
- 회원 관리 기능: 비밀번호 변경 · 탈퇴 · 이메일 인증 · 토큰 취소(로그아웃 서버 처리)는 없습니다. 회원가입은 아이디 · 비밀번호 · 이름만 받습니다
