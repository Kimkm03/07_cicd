// src/api/bank.js — 백엔드 호출은 한 파일에 모은다 (exchange.js 와 같은 패턴)
// 모든 호출이 request() 하나를 거치게 만들어 둔다. 그래서 공통으로 해야 할 일(헤더 · 에러 처리 · 로그인 토큰)을 한 곳에서만 고치면 된다

// 서버 주소. 서버에 CORS 설정(common/WebConfig)이 있어서 브라우저가 8080 으로 바로 부를 수 있다.
// 주소를 바꾸고 싶으면 frontend/.env 에 VITE_API_BASE=... 를 적는다 (적지 않으면 오른쪽의 기본값).
// (참고: CORS 설정 없이 개발하고 싶다면 vite.config.js 의 server.proxy 로 "/api" 를 8080 으로 넘기는 방법도 있다. 우리는 CORS 를 직접 만나 보려고 쓰지 않는다)
const BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080/api";

// 로그인 토큰. 변수(메모리)에만 보관한다 → 새로고침하면 사라진다 (localStorage 에 두면 XSS 로 스크립트가 훔쳐 갈 수 있어서 쓰지 않는다)
let accessToken = null;
// 서버가 401(토큰 만료 · 위조)을 주면 부를 함수. 로그인 상태를 관리하는 쪽(AuthProvider)이 등록한다
let onUnauthorized = () => {};
export const setAccessToken = (token) => { accessToken = token; };
export const setOnUnauthorized = (fn) => { onUnauthorized = fn; };

async function request(path, options = {}) {
  let res;
  try {
    res = await fetch(`${BASE}${path}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...(accessToken && { Authorization: `Bearer ${accessToken}` }),   // 토큰이 있을 때만, 모든 요청에 자동으로 붙는다
        ...options.headers,
      },
    });
  } catch {
    // 서버가 꺼져 있거나 CORS 로 막히면 fetch 자체가 실패한다 (응답이 아예 없다)
    const err = new Error("서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
    err.code = "NETWORK";
    throw err;
  }
  if (res.status === 401) onUnauthorized();
  if (!res.ok) throw await toError(res);
  return res.status === 204 ? null : res.json();
}

// 서버의 ErrorResponse {code, message} 를 그대로 살린다 + 문의용 요청 ID 도 같이
async function toError(res) {
  const body = await res.json().catch(() => ({}));
  const err = new Error(body.message ?? "요청 실패 " + res.status);
  err.code = body.code;
  err.status = res.status;
  err.requestId = res.headers.get("X-Request-Id");   // 서버의 exposedHeaders 덕분에 JS 에서 읽을 수 있다
  return err;
}

// 로그인 (POST /api/auth/login) → { accessToken, expiresIn, name }
// 내 정보 (GET /api/me) → { name, grade }. 하드코딩하던 "김연지 / 우수" 를 서버가 알려 준다
export const login = (loginId, password) =>
  request("/auth/login", { method: "POST", body: JSON.stringify({ loginId, password }) });
export const getMe = () => request("/me");
// 회원가입 (POST /api/auth/signup) → 201 { customerId, loginId, name, accountNo }. 아이디가 이미 있으면 409
export const signup = (loginId, password, name) =>
  request("/auth/signup", { method: "POST", body: JSON.stringify({ loginId, password, name }) });

// 계좌 · 거래내역 (GET)
export const fetchAccounts = () => request("/accounts");
export const fetchTransactions = (accountNo, page = 0) =>
  request(`/accounts/${accountNo}/transactions?page=${page}&size=20`);

// 이체 (POST) — Header 는 봉투 겉면, Body 는 속지
export const transfer = ({ fromAccountNo, toAccountNo, amount, memo }, idempotencyKey) =>
  request("/transfers", {
    method: "POST",
    headers: { "Idempotency-Key": idempotencyKey },   // Header: 이 요청의 번호표 (⚠️ 지금 서버는 이 헤더를 읽지 않고 무시한다 → 원리는 Day 1 멱등성 참고)
    body: JSON.stringify({ fromAccountNo, toAccountNo, amount, memo }),   // Body: 이체 내용
  });

// 입금 (POST) — 금액만 보내면 된다. 메모 · 거래처 · 카테고리는 선택
export const deposit = (accountNo, amount) =>
  request(`/accounts/${accountNo}/deposits`, {
    method: "POST",
    body: JSON.stringify({ amount }),
  });

// AI 카테고리 분류 (POST) — 메모를 보내면 { memo, category } 가 온다. 서버가 AI 를 부르므로 화면에는 AI 키가 없다
export const classifyMemo = (memo) =>
  request("/ai/category", { method: "POST", body: JSON.stringify({ memo }) });

// AI 저축 도우미 (POST) — 질문 하나를 보내면 { reply } 가 온다. 서버가 고객별로 대화를 기억하므로 이번 질문 하나만 보낸다
export const chat = (message) =>
  request("/ai/chat", { method: "POST", body: JSON.stringify({ message }) });

// 새 대화 (DELETE) — 서버가 기억하던 내 대화를 지운다 (204)
export const clearChat = () => request("/ai/chat", { method: "DELETE" });
