// 제목이 있는 섹션(패널)을 구분하는 컴포넌트
import { useAuth } from '../hooks/useAuth.js'

// components/Panel.jsx
function Panel({ title, children }) {

  const { user } = useAuth();      // 로그인한 사람의 이름. '내 계좌' 패널에서만 사용한다

  return (
    <section className="panel">
      {/* 로그인 화면(LoginPage)에서도 Panel 을 쓰므로, user 가 아직 없을 수 있다 */}
      <h2>{ title === '내 계좌' ? `${user.name}의 계좌` : title }</h2>
      {children}
    </section>
  );
}

export default Panel;
