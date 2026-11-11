// src/AccountCard2.jsx
import { useState, useEffect } from 'react'

export default function AccountCard2({ accountId, isLoggedIn }) {
  const [balance, setBalance] = useState(0)
  // 아래처럼 무시해줘라고 주석으로 적어줍니다.
  // eslint-disable-next-line no-unused-vars    
  const 이자율 = 0.031                       // ① 만들어 놓고 쓰지 않음

  if (isLoggedIn) {
    const [memo, setMemo] = useState('')     // ② 훅을 if 안에서 부름
    console.log(memo, setMemo)
  }

  useEffect(() => {
    fetch(`/api/accounts/${accountId}`)
      .then((r) => r.json())
      .then((d) => setBalance(d.balance))
  }, [])                                     // ③ accountId 가 배열에 없음

  return (
    <div>
      <p>{balance.toLocaleString()}원</p>
    </div>
  )
}