// components/ChatBot.jsx — 화면 오른쪽 아래에 붙어 있는 AI 저축 도우미 (Day 3 끝, 서버의 POST /api/ai/chat)
// 숫자는 서버의 리포트 API 값(DB 가 계산)이고, 문장만 AI 가 만든다
import { useEffect, useRef, useState } from 'react'
import { chat, clearChat } from '../api/bank.js'

const GREETING = { role: 'bot', text: "안녕하세요! 소비 리포트, 저축 계획, 은행 안내(적금 · 휴면 · 이체 규칙)를 물어보세요." }
const QUICK = ["지난달 소비 리포트", "한 달에 얼마 모을 수 있을까?", "내 계좌 잔액 알려줘", "휴면 계좌는 어떻게 풀어?"]   // 마지막은 은행 안내문(RAG) 질문

function ChatBot() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState([GREETING])
  const [input, setInput] = useState("")
  const [sending, setSending] = useState(false)   // AI 응답은 몇 초 걸릴 수 있다 → 그동안 보내기를 잠근다
  const listRef = useRef(null)

  // 새 메시지가 생기면 맨 아래로 스크롤 (화면 요소를 직접 움직이는 일이라 useEffect 에서 한다)
  useEffect(() => {
    if (listRef.current) listRef.current.scrollTop = listRef.current.scrollHeight
  }, [messages, open])

  // 봇 말풍선 하나 추가 (답 · 에러 모두 이걸로)
  const addBot = (text) => setMessages((m) => [...m, { role: 'bot', text }])

  async function send(text) {
    const question = text.trim()
    if (!question || sending) return
    setMessages((m) => [...m, { role: 'user', text: question }])
    setInput("")
    setSending(true)
    try {
      // 서버가 고객별로 대화를 기억한다 → 화면은 이번 질문 하나만 보낸다
      const { reply } = await chat(question)
      addBot(reply)
    } catch (err) {
      addBot(err.message)
    } finally {
      setSending(false)
    }
  }

  // 새 대화: 서버가 기억하던 대화를 지우고 화면도 처음으로
  async function handleNewChat() {
    try {
      await clearChat()
      setMessages([GREETING])
    } catch (err) {
      addBot(err.message)
    }
  }

  if (!open) {
    return (
      <button className="btn chatbot-toggle" onClick={() => setOpen(true)}>💬 저축 도우미</button>
    )
  }

  return (
    <div className="chatbot">
      <div className="chatbot-header">
        <strong>AI 저축 도우미</strong>
        <button className="chatbot-new" onClick={handleNewChat} disabled={sending}>새 대화</button>
        <button className="chatbot-close" onClick={() => setOpen(false)} aria-label="닫기">✕</button>
      </div>

      <div className="chatbot-list" ref={listRef}>
        {messages.map((m, i) => (
          <div key={i} className={`chatbot-msg ${m.role}`}>{m.text}</div>
        ))}
        {sending && <div className="chatbot-msg bot">생각 중...</div>}
      </div>

      <div className="chatbot-chips">
        {QUICK.map((q) => (
          <button key={q} className="chip" disabled={sending} onClick={() => send(q)}>{q}</button>
        ))}
      </div>

      <form className="chatbot-form" onSubmit={(e) => { e.preventDefault(); send(input) }}>
        <input value={input} onChange={(e) => setInput(e.target.value)} placeholder="궁금한 것을 물어보세요" />
        <button className="btn" type="submit" disabled={sending || input.trim() === ""}>보내기</button>
      </form>
    </div>
  )
}

export default ChatBot
