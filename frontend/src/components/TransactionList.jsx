import { useState } from 'react'
import TransactionRow from './TransactionRow'
import { fetchTransactions } from '../api/bank.js'
import { useFetch } from '../hooks/useFetch.js'
import { formatWon } from '../utils/format.js'

const TYPE_OPTIONS = ["전체", "입금", "출금"]

const CATEGORY_OPTIONS = [
  "전체",
  "식비",
  "교통",
  "쇼핑",
  "급여",
  "이체",
  "의료",
  "통신",
]
function TransactionList({ accountNo, refreshKey, showAmount }) {
  // 어느 계좌의 거래인지(accountNo)를 받아 서버에서 불러온다.
  // 두 번째 인자 [accountNo, refreshKey]: 이 값이 바뀌면 다시 불러온다 (useFetch 의 deps)
  const { data, loading, error, reload } =
    useFetch(() => fetchTransactions(accountNo), [accountNo, refreshKey]);

  // 🚨 훅(useState)은 아래의 로딩 · 에러 return 보다 위에 있어야 한다
  const [typeFilter, setTypeFilter] = useState("전체")
  const [categoryFilter, setCategoryFilter] = useState("전체")

  if (loading) return <p className="muted">거래 내역을 불러오는 중...</p>;
  if (error) return (
    <>
      <p className="muted">거래 내역을 불러오지 못했습니다. ({error.message})</p>
      <button className="btn" onClick={reload}>다시 시도</button>
    </>
  );

  const transactions = data.content   // 서버가 페이징 응답({content: [...], page: {...}})으로 준다
  const visibleTransactions = transactions.filter((tx) => {
    const matchesType =
      typeFilter === "전체" || tx.txType === typeFilter

    const matchesCategory =
      categoryFilter === "전체" || tx.category === categoryFilter

    return matchesType && matchesCategory
  })

  const totalAmount = Math.abs(
    visibleTransactions.reduce((total, tx) => {
      const signedAmount =
        tx.txType === "입금" ? tx.amount : -tx.amount

      return total + signedAmount
    }, 0)
  )

  return (
    <>
      <div className="chips">
        {TYPE_OPTIONS.map((type) => (
          <button
            key={type}
            className={`chip ${typeFilter === type ? "on" : ""}`}
            onClick={() => setTypeFilter(type)}
          >
            {type}
          </button>
        ))}
      </div>

      <div className="chips">
        {CATEGORY_OPTIONS.map((category) => (
          <button
            key={category}
            className={`chip ${categoryFilter === category ? "on" : ""}`}
            onClick={() => setCategoryFilter(category)}
          >
            {category}
          </button>
        ))}
      </div>

      <div className="summary">
        총 {visibleTransactions.length}건 · 합계 {formatWon(totalAmount)}
      </div>

      {visibleTransactions.map((tx) => (
        <TransactionRow
          key={tx.txId}
          {...tx}
          hideAmount={showAmount}
        />
      ))}
    </>
  )
}

export default TransactionList
