# won-banking-mini — 원뱅킹 React 프론트엔드

React 19 + Vite 8 (JavaScript, ESLint). 백엔드(Spring Boot, 이 레포의 루트)의 REST API 를 부르는 화면입니다.

## 실행하기

백엔드를 먼저 띄웁니다. 프로필도 환경변수도 필요 없습니다 (메모리 DB + 데모 데이터가 기본입니다).

```bash
# 프로젝트 루트(= build.gradle 이 있는 곳)에서
./gradlew bootRun
# 또는 IntelliJ 에서 WonitApplication 실행
```

그다음 이 폴더에서 React 를 띄웁니다.

```bash
cd frontend
npm ci          # package-lock.json 에 적힌 버전 그대로 설치 (처음 한 번)
npm run dev     # http://localhost:5173
```

> 주소는 반드시 `http://localhost:5173` 이어야 합니다. 서버의 CORS 설정(`common/WebConfig`)이 이 주소만 허락합니다.
> `npm run dev` 가 5173 이 아닌 다른 포트로 뜨면 다른 프로그램이 5173 을 쓰고 있다는 뜻입니다. 그 프로그램을 끄고 다시 실행하세요.

## 데모 계정

`http://localhost:5173` 을 열면 로그인 화면이 나옵니다. 아이디 · 비밀번호는 서버의 `AuthController` 안에 데모용으로 들어 있습니다.

| 아이디 | 비밀번호 | 이름 | 계좌 |
|---|---|---|---|
| `yeonji` | `1234` | 김연지 | `1002-345-678901` 지급정지 · `1002-345-112233` 정상 · `1002-345-998877` 휴면 |
| `woori` | `1234` | 이우리 | `1002-555-667788` 정상 |

- 로그인하면 서버가 준 토큰(JWT)을 **메모리 변수**(`src/api/bank.js`)에만 보관하고, 이후 모든 요청의 `Authorization: Bearer ...` 헤더에 붙입니다. 그래서 **새로고침하면 다시 로그인**해야 합니다.
- 서버가 401(토큰 만료 · 위조)을 주면 자동으로 로그아웃되어 로그인 화면으로 돌아옵니다.
- 로그인한 사람의 계좌만 보입니다. `woori` 로 로그인하면 김연지의 계좌 3개는 보이지 않습니다.
- 이체 화면에서 **보내는 계좌**를 고를 수 있습니다. 지급정지 계좌에서 보내 보면 서버의 에러 코드(`ACCOUNT_FROZEN`)와 **문의 번호**(서버 로그를 찾는 번호)가 화면에 나옵니다.

## 코드 구조 (`src/`)

| 파일 | 하는 일 |
|---|---|
| `api/bank.js` | 서버 호출 전용. 모든 호출이 `request()` 하나를 거치며 토큰 · 에러(`toError`)를 한 곳에서 처리 |
| `contexts/AuthContext.jsx` · `hooks/useAuth.js` | 로그인 정보. `AuthContext.jsx` 는 Provider 컴포넌트, `useAuth.js` 는 Context 객체와 꺼내 쓰는 훅 (컴포넌트와 같은 파일에 두면 lint 규칙에 걸려서 2개로 나눔) |
| `App.jsx` | 로그인 했으면 `BankHome`, 아니면 `LoginPage` (라우터 없이 조건부 렌더링) |
| `BankHome.jsx` | 은행 화면 (계좌 · 입금 · 이체 · 거래내역 · AI 분류 · 환율 · AI 저축 도우미 채팅창) |
| `components/LoginPage.jsx` | 로그인 폼 |

## 자주 쓰는 명령

| 명령 | 하는 일 |
|---|---|
| `npm run dev` | 개발 서버 (코드를 고치면 화면이 바로 바뀝니다) |
| `npm run lint` | 코드 검사. **error 0 · warning 0** 이어야 합니다 (`AccoundCard2.jsx` 는 일부러 틀린 연습 파일이라 검사에서 제외) |
| `npm run build` | 배포용 파일을 `dist/` 에 만듭니다 |

## 환경변수 `VITE_API_BASE`

백엔드 주소의 기본값은 `http://localhost:8080/api` 입니다. 다른 주소를 쓰려면 `frontend/.env` 파일을 만들고 적습니다.

```
VITE_API_BASE=http://localhost:8080/api
```

- 값을 바꾼 뒤에는 `npm run dev` 를 껐다 다시 켜야 반영됩니다.
- `VITE_` 로 시작하는 환경변수는 **브라우저 코드에 그대로 들어갑니다.** 비밀번호나 API 키를 넣지 마세요.
