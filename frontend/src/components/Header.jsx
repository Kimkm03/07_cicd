import { useAuth } from '../hooks/useAuth.js'

// 로그인한 사람의 이름은 AuthContext 에서 꺼내 씁니다 (useAuth). 서버(GET /api/me)가 알려 준 값입니다.
function Header() {
    const { user, logout } = useAuth();

    return (
        <div className="row">
            <h1>{user.name}님, 안녕하세요</h1>
            <button className="btn" onClick={logout}>로그아웃</button>
        </div>
    )
}

export default Header;
