// 이체 부하테스트 (k6): 클라이언트가 "계좌 목록 보기 → 10원 이체" 를 반복하는 모습을 여러 명이 동시에 흉내 낸다
// 실행 (모니터링 EC2 에서, k6 설치 없이 docker): ALB 주소로 건다 (CloudFront 도메인으로 걸면 막힐 수 있다)
//   가볍게:   docker run --rm -i -e BASE_URL=http://<ALB DNS 이름> -e PEAK_VUS=10 -e HOLD=20s grafana/k6 run - < load/transfers.js
//   자동 확장: docker run --rm -i -e BASE_URL=http://<ALB DNS 이름> -e PEAK_VUS=50 -e HOLD=8m  grafana/k6 run - < load/transfers.js
//             (경보 약 5분 + 새 서버 2~3분 → 8분이면 늘어난 서버가 부하를 나눠 받는 모습까지 보인다. 끝나고 약 5분 뒤 저절로 줄어든다)
//   모니터링 EC2 에 SSH 가 막히면 CloudShell 에서: bash load/cloudshell-ssh.sh <이름>
//   ALB 보안 그룹 80 에 모니터링 EC2 의 탄력적 IP/32 는 스택이 이미 허용해 두었다 (그 밖에는 CloudFront 만)
//   내 PC 에서 연습: ./gradlew bootRun 후 -e BASE_URL=http://host.docker.internal:8080 --add-host=host.docker.internal:host-gateway
// 🚨 /api/ai/* 는 넣지 않는다: 요청마다 외부 AI 요금이 나간다
// 모든 이체가 같은 계좌(A)에서 나간다 → 비관적 락 때문에 줄을 서서 기다린다. 이때 늘어나는 것이 응답 시간이다
import http from 'k6/http';
import { check, fail, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PEAK = parseInt(__ENV.PEAK_VUS || '50');   // 동시에 일하는 가상 클라이언트 수의 꼭대기
const HOLD = __ENV.HOLD || '40s';                // 꼭대기에서 버티는 시간

export const options = {
  stages: [
    { duration: '15s', target: PEAK },   // 천천히 늘린다 (한꺼번에 몰면 어디서 느려지는지 알 수 없다)
    { duration: HOLD, target: PEAK },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],   // 요청의 95% 는 1초 안에
    checks: ['rate>0.99'],
  },
};

// 시작할 때 한 번만 준비한다 (RDS 에는 데모 데이터가 없다 → 클라이언트 두 명을 스스로 가입시킨다)
//   A: 이체를 보내는 클라이언트 (100만 원 입금), B: 받는 클라이언트. VU 마다 로그인하면 BCrypt 검사가 부하의 대부분이 되므로 로그인도 한 번만
export function setup() {
  const json = { headers: { 'Content-Type': 'application/json' } };
  const tag = Date.now();                                   // 실행할 때마다 다른 아이디
  const signup = (id) => {
    const r = http.post(`${BASE_URL}/api/auth/signup`, JSON.stringify({ loginId: id, password: 'k6pass1234', name: 'k6' }), json);
    if (r.status !== 201) fail(`가입 실패 ${id} status=${r.status} body=${r.body}`);
    return r.json('accountNo');                             // 가입하면 0원 입출금 계좌가 하나 열린다
  };
  const fromAccountNo = signup(`k6a${tag}`);
  const toAccountNo = signup(`k6b${tag}`);

  const login = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({ loginId: `k6a${tag}`, password: 'k6pass1234' }), json);
  if (login.status !== 200) fail(`로그인 실패 status=${login.status} body=${login.body}`);
  const token = login.json('accessToken');

  const dep = http.post(`${BASE_URL}/api/accounts/${fromAccountNo}/deposits`, JSON.stringify({ amount: 1000000, memo: 'k6 준비' }),
    { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } });
  if (dep.status !== 201) fail(`입금 실패 status=${dep.status} body=${dep.body}`);
  return { token, fromAccountNo, toAccountNo };
}

export default function (data) {
  const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${data.token}` };

  const list = http.get(`${BASE_URL}/api/accounts`, { headers });
  check(list, { '계좌 목록 200': (r) => r.status === 200 });

  const body = JSON.stringify({ fromAccountNo: data.fromAccountNo, toAccountNo: data.toAccountNo, amount: 10 });
  const res = http.post(`${BASE_URL}/api/transfers`, body,
    { headers: { ...headers, 'Idempotency-Key': crypto.randomUUID() } });  // 요청마다 다른 키 (서버는 아직 읽지 않지만 화면과 같은 모양으로 보낸다)
  check(res, { '이체 201': (r) => r.status === 201 });

  sleep(0.1);
}
