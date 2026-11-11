// 필요한 부품들을 불러옵니다.
import { useState } from 'react'
import LoginPage from './components/LoginPage.jsx'
import SignupPage from './components/SignupPage.jsx'
import BankHome from './BankHome.jsx'
import { useAuth } from './hooks/useAuth.js'

// 라우터(react-router) 없이 조건부 렌더링만으로 화면을 바꿉니다.
// 지금까지의 App 내용은 BankHome.jsx 로 옮겼고, App 은 "로그인 했나?" 만 봅니다.
function App() {
  const { user } = useAuth();            // null 이면 로그인 전
  const [signingUp, setSigningUp] = useState(false);   // 로그인 전 화면 중 어느 쪽? (로그인 / 회원가입)
  if (!user) {
    return signingUp
      ? <SignupPage onBack={() => setSigningUp(false)} />
      : <LoginPage onSignup={() => setSigningUp(true)} />;
  }
  return <BankHome />;                   // 로그인 후에는 은행 화면
}

export default App
