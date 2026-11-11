import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css' // 중복되는 값이 있으면 import 순서에 좌우됩니다.
import App from './App.jsx'
import { AuthProvider } from './contexts/AuthContext.jsx'

const root = createRoot(document.getElementById('root'))
root.render(
  <StrictMode>
    {/* AuthProvider 가 App 전체를 감싸서 어디서든 useAuth() 로 로그인 정보를 꺼낼 수 있다 */}
    <AuthProvider>
      <App />
    </AuthProvider>
  </StrictMode>,
)

// // main.jsx 에서 1초마다 다시 그리기
// setInterval(() => {
//   root.render(<App />);
// }, 1000);
