// src/hooks/useAuth.js : 로그인 Context 객체와, 그것을 꺼내 쓰는 훅
// (Provider 컴포넌트는 contexts/AuthContext.jsx. 컴포넌트와 같은 파일에 두면 Fast Refresh 규칙 react-refresh/only-export-components 에 걸린다)
import { createContext, useContext } from "react";

export const AuthContext = createContext(null);   // null = 아직 Provider 밖

// 어디서든 꺼내 쓰기 — props 로 내려 주지 않아도 된다
export function useAuth() {
  return useContext(AuthContext);     // { user, login, signup, logout }
}
