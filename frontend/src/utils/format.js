// 2. 함수: 여러 컴포넌트에서 재사용하기 위해 공통 저장소에 함수를 뺌
//          일반 함수와 함수형 컴포넌트의 문법이 같아서 헷갈리는 일도 막을 수 있을 겁니다.

// export 키워드로 외부에서 불러올 수 있게 선언 
// 계좌 잔액을 "1,523,000원" 형태의 문자열로 바꿔주는 함수
export function formatWon(amount) {
  return amount.toLocaleString("ko-KR") + "원"
}

// 계좌번호 앞부분을 가리고 마지막 한 자리만 보여주는 함수
// 예) "1002-345-678901" -> "1002-345-6****1"
export function maskAccountNo(no) {
  return no.slice(0, -5) + "****" + no.slice(-1)
}

// hide 가 true 면 실제 금액 대신 "••••••원" 을 보여줍니다.
export function formatWonMasked(amount, hide) {
  return hide ? "••••••원" : formatWon(amount)
}
