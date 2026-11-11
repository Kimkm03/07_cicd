// src/hooks/useFetch.js
import { useState, useEffect } from "react";

// 외부에서 데이터를 불러올 때 성공/실패/로딩중 화면을 만드는 작업을
export function useFetch(fetcher, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let alive = true;             // 정리용 flag
    // 처음에는 loading 의 처음값이 이미 true 다. "다시 시도" 때의 초기화는 아래 reload 가 맡는다
    // (effect 안에서 곧바로 setState 를 부르면 화면이 한 번 더 그려져서 여기서는 부르지 않는다)

    // asynchrous  비동기로 특정 함수를 실행하기 위한 키워드
    async function load() {
      try {
        const result = await fetcher();
        if (alive) setData(result);
      } catch (e) {
        if (alive) setError(e);          // 메시지 문자열이 아니라 에러 객체째로 (code · requestId 를 화면에서 쓰기 위해). 메시지는 error.message
      } finally {
        if (alive) setLoading(false);
      }
    }
    load();

    return () => { alive = false;  }; 
  // fetcher 는 그릴 때마다 새로 만들어지는 함수라 일부러 뺀다. 다시 불러올 때는 deps · reloadKey 가 바뀐다
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reloadKey, ...deps]); // dependency(의존) 해당 state가 변경될 때마다 새로 컴포넌트가 엘리먼트를 찍어내도록 걸어주는 변수

  // "다시 시도" 를 누르는 순간 화면을 로딩 상태로 되돌린다 (이벤트 핸들러 안의 setState 는 괜찮다)
  const reload = () => {
    setLoading(true);
    setError(null);
    setReloadKey((k) => k + 1);
  };
  return { data, loading, error, reload };
}