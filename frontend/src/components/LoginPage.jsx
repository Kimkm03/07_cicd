// components/LoginPage.jsx — TransferForm 과 같은 패턴 (폼 state 객체 하나 · handleChange 하나 · preventDefault · sending 잠금 · 에러 문구)
import { useState } from "react"
import Panel from "./Panel.jsx"
import { useAuth } from "../hooks/useAuth.js"

function LoginPage({ onSignup }) {
  const { login } = useAuth()
  const [form, setForm] = useState({ loginId: "", password: "" })   // 입력값 두 개를 state 객체 하나로
  const [sending, setSending] = useState(false)
  const [errorMessage, setErrorMessage] = useState("")

  function handleChange(e) {
    const { name, value } = e.target
    setForm({ ...form, [name]: value })
  }

  async function handleSubmit(e) {
    e.preventDefault()                // 폼 제출 때 페이지가 새로고침되는 기본 동작을 막는다
    setSending(true)
    setErrorMessage("")
    try {
      await login(form.loginId, form.password)
    } catch (err) {
      setErrorMessage(err.message)     // 서버가 준 "아이디 또는 비밀번호가 올바르지 않습니다." (toError 가 서버 메시지를 담아 던진다)
      setForm({ ...form, password: "" })   // 틀렸으면 비밀번호 칸만 비운다
    } finally {
      setSending(false)
    }
  }

  const canSubmit = form.loginId.trim() !== "" && form.password !== ""

  return (
    <Panel title="로그인">
      <form onSubmit={handleSubmit}>
        <div className="field">
          <label>아이디</label>
          <input type="text" name="loginId" value={form.loginId} onChange={handleChange} autoComplete="username" />
        </div>
        <div className="field">
          <label>비밀번호</label>
          {/* 🚨 비밀번호 칸은 type="password". console.log 로 찍거나 localStorage 에 저장하지 않는다 */}
          <input type="password" name="password" value={form.password} onChange={handleChange} autoComplete="current-password" />
        </div>
        {errorMessage !== "" && <p className="error">{errorMessage}</p>}
        <button className="btn" type="submit" disabled={sending || !canSubmit}>
          {sending ? "로그인 중..." : "로그인"}
        </button>
      </form>
      <p className="auth-switch">
        처음이신가요? <button className="btn btn-ghost" type="button" onClick={onSignup}>회원가입</button>
      </p>
    </Panel>
  )
}

export default LoginPage
