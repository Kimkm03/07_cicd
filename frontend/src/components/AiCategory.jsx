// components/AiCategory.jsx — 거래 메모를 AI 에게 보내 카테고리를 받아 온다 (Day 1 Spring AI 의 POST /api/ai/category)
import { useState } from 'react'
import { classifyMemo } from '../api/bank.js'

function AiCategory() {
  const [memo, setMemo] = useState("")
  const [result, setResult] = useState(null)     // 서버 응답 { memo, category }
  const [error, setError] = useState(null)
  const [sending, setSending] = useState(false)  // AI 응답은 몇 초 걸릴 수 있다 → 그동안 버튼을 잠근다

  async function handleSubmit(e) {
    e.preventDefault()
    setSending(true)
    setError(null)
    try {
      setResult(await classifyMemo(memo))
    } catch (err) {
      setError(err)
    } finally {
      setSending(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <div className="field">
        <label>거래 메모</label>
        <input value={memo} onChange={(e) => setMemo(e.target.value)} placeholder="예: 스타벅스 강남점" />
      </div>
      <button className="btn" type="submit" disabled={sending || memo.trim() === ""}>
        {sending ? "분류 중..." : "AI 분류"}
      </button>
      {/* 키가 없거나 AI 호출이 실패해도 서버는 "기타" 로 답한다 (서버 로그에 WARN) */}
      {result && <p>"{result.memo}" → <strong>{result.category}</strong></p>}
      {error && <p className="error">{error.message}</p>}
    </form>
  )
}

export default AiCategory
