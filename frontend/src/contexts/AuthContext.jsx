// src/contexts/AuthContext.jsx : 컴포넌트(AuthProvider)만 export 한다
// "지금 누가 로그인했나" 는 앱 전체가 공유하는 값이라 Context 에 담는다 (예전 UserContext 를 승격한 것)
import { useEffect, useState } from "react";
import { login as loginApi, signup as signupApi, getMe, setAccessToken, setOnUnauthorized } from "../api/bank.js";
import { AuthContext } from "../hooks/useAuth.js";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);       // null = 로그인 안 함 (flag 변수 원칙: 기본값은 꺼진 상태)

  async function login(loginId, password) {
    const { accessToken } = await loginApi(loginId, password);
    setAccessToken(accessToken);                 // 이후 모든 요청에 자동으로 붙는다
    try {
      const me = await getMe();                  // GET /api/me : 하드코딩이던 { name, grade } 를 서버가 알려 준다
      setUser(me);
    } catch (err) {
      setAccessToken(null);                      // 내 정보를 못 받았으면 로그인 안 한 상태로 되돌린다
      throw err;
    }
  }

  // 가입이 끝나면 같은 아이디 · 비밀번호로 바로 로그인한다 (가입 응답에는 토큰이 없다)
  async function signup(loginId, password, name) {
    await signupApi(loginId, password, name);
    await login(loginId, password);
  }

  function logout() {
    setAccessToken(null);
    setUser(null);
  }

  useEffect(() => {
    // 서버가 401(토큰 만료 · 위조)을 주면 자동 로그아웃
    setOnUnauthorized(() => {
      setAccessToken(null);
      setUser(null);
    });
    return () => setOnUnauthorized(() => {});
  }, []);

  return <AuthContext.Provider value={{ user, login, signup, logout }}>{children}</AuthContext.Provider>;
}
