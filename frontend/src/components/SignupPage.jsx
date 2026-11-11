// components/SignupPage.jsx — LoginPage 와 같은 패턴 (폼 state 객체 하나 · handleChange 하나 · sending 잠금 · 에러 문구)
import { useState } from "react"
import Panel from "./Panel.jsx"
import { useAuth } from "../hooks/useAuth.js"

function SignupPage({ onBack }) {
  const { signup } = useAuth()
  const [form, setForm] = useState({ loginId: "", password: "", name: "" })
  const [sending, setSending] = useState(false)
  const [errorMessage, setErrorMessage] = useState("")

  function handleChange(e) {
    const { name, value } = e.target
    setForm({ ...form, [name]: value })
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setSending(true)
    setErrorMessage("")
    try {
      await signup(form.loginId, form.password, form.name)   // 가입 → 바로 로그인 → 은행 화면 (0원 입출금 계좌가 하나 생겨 있다)
      onBack()                         // 다음에 로그아웃하면 회원가입이 아니라 로그인 화면이 보이게 되돌려 둔다
    } catch (err) {
      setErrorMessage(err.message)     // "이미 있는 아이디입니다." 같은 서버 메시지
    } finally {
      setSending(false)
    }
  }

  // 서버도 같은 규칙(@Size(min = 4))으로 한 번 더 막는다. 화면 검사는 편의, 진짜 검사는 서버
  const canSubmit = form.loginId.trim() !== "" && form.password.length >= 4 && form.name.trim() !== ""

  return (
    <Panel title="회원가입">
      <form onSubmit={handleSubmit}>
        <div className="field">
          <label>아이디</label>
          <input type="text" name="loginId" value={form.loginId} onChange={handleChange} autoComplete="username" />
        </div>
        <div className="field">
          <label>비밀번호 (4자 이상)</label>
          <input type="password" name="password" value={form.password} onChange={handleChange} autoComplete="new-password" />
        </div>
        <div className="field">
          <label>이름</label>
          <input type="text" name="name" value={form.name} onChange={handleChange} autoComplete="name" />
        </div>
        {errorMessage !== "" && <p className="error">{errorMessage}</p>}
        <div className="form-actions">
          <button className="btn" type="submit" disabled={sending || !canSubmit}>
            {sending ? "가입 중..." : "가입하기"}
          </button>
          <button className="btn btn-ghost" type="button" onClick={onBack}>로그인으로</button>
        </div>
      </form>
    </Panel>
  )
}

export default SignupPage
