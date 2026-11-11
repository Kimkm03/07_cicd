 // /src/api/exchange.js

// 환율 API. 
const API_URL = "https://open.er-api.com/v6/latest/USD";

// 실제 서버는 많은 사람들의 요청을 동시에 받습니다.
// 이 함수가 외부서버에 가서 결과를 받아올 때까지는 
// async로 작성한 함수 안에  
export async function fetchUsdKrw() {
             // await으로 걸어놓은 동작들은 
             // 인터프리터가 지켜보지 않습니다.
  const res = await fetch(API_URL);
  if (!res.ok) throw new Error("응답 오류 " + res.status);
  const data = await res.json();
  return data.rates.KRW;
}

