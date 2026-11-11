import { useState } from 'react'; //  소문자로 적어줍니다.

// state는 컴포넌트 안에서 사용하고 관리되는 데이터 변수입니다.
// 함수형 컴포넌트 Counter 를 선언
function Counter() {
    // const는 기본자료형에 대해서 한번 방을 만들면 값을 바꿀 수 없습니다.
    // 그러나 이 아래 방들은 Array: 참조자료형이기 때문에 값을 바꿀 수 있습니다.
    // 각 배열의 위치를 구조분해해서 count / setCount 라는 변수로 직접 꺼내 씁니다.

    const [count, setCount] = useState(0); // 초기값 

    return (<>
    <button onClick={() => setCount(count + 1)}> {count} </button>
    </> )
}

export default Counter