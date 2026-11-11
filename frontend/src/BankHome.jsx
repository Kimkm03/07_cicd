// 필요한 부품들을 불러옵니다.
import './App.css'
import Clock from './components/Clock.jsx'
import Panel from './components/Panel.jsx'
import AccountCard  from './components/AccountCard.jsx'
import Header from './components/Header'
import { useState } from 'react'
import { formatWon } from './utils/format.js'
import ExchangeRate from './components/ExchangeRate.jsx'
import TransactionList from './components/TransactionList.jsx'
import TransferForm from './components/TransferForm.jsx'
import AiCategory from './components/AiCategory.jsx'
import ChatBot from './components/ChatBot.jsx'
import { fetchAccounts, transfer, deposit } from './api/bank.js'   // 서버 호출은 api/bank.js 한 곳에 모아 두었다
import { useFetch } from './hooks/useFetch.js'

// 로그인한 뒤 보이는 은행 홈 화면을 그립니다.
function BankHome() {
  
  // 화면이 렌더링 되기 위해 필요로 하는 값(data)을 적습니다.
  // 1. 데이터
  // 계좌 목록은 이제 가짜(mock) 배열이 아니라 서버(GET /api/accounts)에서 받아 옵니다.
  // useFetch 가 data(결과) · loading(불러오는 중) · error(실패) · reload(다시 불러오기)를 만들어 줍니다.
  const { data: accounts, loading, error, reload } = useFetch(fetchAccounts);

  // flag 변수: 깃발을 들어서 교통량을 제어하는 것처럼 이 변수의 역할은 특정 로직을 끄거나 켜거나 밖에 없기 때문에
  // flag 변수를 사용할 때는 default 값을 false로 만들고 시작하는 로직을 권장 
  const [showFullNo, setShowFullNo] = useState(false);
  //     ↑현재 값      ↑바꾸는 함수              ↑처음값

  // 금액 보기 / 숨기기 (AccountCard · TransactionList 에 prop 으로 넘긴다)
  const [showAmount, setShowAmount] = useState(false);

  // 이체 · 입금이 끝나면 이 숫자를 1 올립니다. TransactionList 가 이 값이 바뀐 것을 보고 거래 내역을 다시 불러옵니다.
  const [refreshKey, setRefreshKey] = useState(0);

  // 어느 계좌에서 돈을 보낼지 고른 계좌번호. null 이면 첫 번째 계좌.
  // (과제 정답 예시: 출금 계좌를 고르는 선택 상자. 처음에는 1번 계좌 = 지급정지 계좌라 에러 화면을 만들어 보기 좋다)
  const [fromAccountNo, setFromAccountNo] = useState(null);

  // 입금 버튼을 눌렀을 때 서버가 거절하면 그 이유를 담아 둡니다 (없으면 null)
  const [depositError, setDepositError] = useState(null);

  // 🚨 훅(useState, useFetch)은 아래의 로딩 · 에러 return 보다 위에 있어야 합니다.
  // 처음 불러오는 동안만 로딩 화면 (이체 후 다시 불러올 때는 화면을 유지한다)
  if (loading && !accounts) return <p className="muted">계좌를 불러오는 중...</p>;
  if (error && !accounts) return (
    <>
      <p className="muted">계좌를 불러오지 못했습니다. ({error.message})</p>
      <button className="btn" onClick={reload}>다시 시도</button>
    </>
  );

  // 고른 계좌를 찾는다. 아직 안 골랐으면 첫 번째 계좌
  const fromAccount = accounts.find((a) => a.accountNo === fromAccountNo) ?? accounts[0]

  // 입금 버튼: 서버에 입금을 요청하고, 끝나면 서버 데이터로 다시 그립니다 (화면에서 balance + 10000 을 직접 계산하지 않는다)
  // accountId 는 서버 응답에 없으므로 accountNo 로 계좌를 구분합니다.
  async function handleDeposit(accountNo) {
    setDepositError(null)
    try {
      await deposit(accountNo, 10000)
      reload()                         // 계좌 목록 다시 불러오기
      setRefreshKey((v) => v + 1)      // 거래 목록도 다시 불러오기
      setFromAccountNo(accountNo)      // 아래 "최근 거래" 는 고른 계좌 하나의 거래만 보여 준다 → 입금한 계좌로 바꿔야 방금 입금이 보인다
    } catch (err) {
      setDepositError(err)
    }
  }

  // 이체 폼(TransferForm)에서 이체 버튼을 누르면 이 함수가 실행됩니다.
  // 서버에 보내고, 끝나면 서버가 알려 주는 값으로 다시 그립니다. 잔액을 화면에서 직접 계산하지 않습니다
  // (화면의 계산과 서버의 진짜 잔액이 달라질 수 있기 때문입니다).
  // 실패하면 에러를 그대로 던집니다 → TransferForm 이 받아서 에러 문구와 문의 번호를 보여 줍니다.
  async function handleTransfer({ toAccount, amount, memo }, idempotencyKey) {
    await transfer(
      { fromAccountNo: fromAccount.accountNo, toAccountNo: toAccount, amount, memo },
      idempotencyKey,
    )
    reload()                           // 계좌 목록 다시 불러오기
    setRefreshKey((v) => v + 1)        // 거래 목록도 다시 불러오기
  }

  // 합계를 state로 두지 않습니다. component 안에서의 각각의 상태값이 아니고
  // 그릴 때마다 다시 계산하는 변수
  // reduce 로 더하므로 계좌가 몇 개든 동작합니다 (accounts[0] + accounts[1] + ... 으로 쓰면 계좌 수가 바뀔 때 깨집니다)
  const totalBalance = accounts.reduce((sum, a) => sum + a.balance, 0)

  // XML에서는 여는 꺽쇠 안의 태그가 무엇이든 될 수 있기 때문에 <이름>김연지 </이름>
  // JSX 가 소문자 태그는 HTML, 대문자로 시작하는 태그는 컴포넌트로 인식
  // return ( ) 바깥에서는 일반 자바스크립트처럼 // 로 주석을 적습니다.
  // return 뒤에 렌더링 될 부분을 적습니다.
  return (
    <> 
    {/* 로그인한 사람의 이름은 Header · Panel 이 useAuth() 로 직접 꺼내 씁니다 (AuthProvider 는 main.jsx 에 있다) */}
    <Header />

    <button onClick={() => setShowFullNo(!showFullNo)}>
      {/* 논리연산자를 사용해서 같은 화면을 조건부 렌더링해보세요 */}
      {/* showFullNo ? "계좌번호 숨기기" : "계좌번호 보기" */}
      {showFullNo && "계좌번호 숨기기"}
      {!showFullNo && "계좌번호 보기"}
    </button>

    <button onClick={() => setShowAmount(!showAmount)}>
      {showAmount ? "금액 숨기기" : "금액 보기"}
    </button>
    
    <Clock />
    {/* class 는 JS의 예약어이므로 JSX에서는 className으로 대신 사용합니다.*/}

    <Panel title="이체">
      <div className="field">
        <label>보내는 계좌</label>
        <select value={fromAccount.accountNo} onChange={(e) => setFromAccountNo(e.target.value)}>
          {accounts.map((a) => (
            <option key={a.accountNo} value={a.accountNo}>
              {a.accountNo} ({a.accountType} · {a.status})
            </option>
          ))}
        </select>
      </div>
      <TransferForm fromAccount={fromAccount} onTransfer={handleTransfer} />
    </Panel>
     
    <div className="total">
      <p> 총 자산 </p>
      <p> {formatWon(totalBalance) } </p>
    </div>

    <Panel title={'내 계좌'}>
      {accounts.map((account) => (
        // 계좌의 status 는 Context 가 아니라 props 로 내려 줍니다. (Context 는 "지금 로그인한 사람" 처럼 앱 전체가 공유하는 값에만)
        // key 는 accountId 가 아니라 accountNo (서버 응답에는 accountId 가 없다)
        <AccountCard key={account.accountNo}
                     showFullNo={showFullNo}
                     showAmount={showAmount}
                     onDeposit={() => handleDeposit(account.accountNo)}
                     accountNo={account.accountNo}
                     accountType={account.accountType}
                     balance={account.balance}
                     status={account.status}
                     />
        ))}
      {/* 입금이 거절되면(예: 휴면 계좌 409) 이유와 문의 번호를 보여 줍니다 */}
      {depositError && (
        <div className="error">
          <p>{depositError.message}</p>
          {depositError.requestId && (
            <small className="muted">문의 번호: {depositError.requestId}</small>
          )}
        </div>
      )}
    </Panel>

    <Panel title={`최근 거래 (${fromAccount.accountNo})`}>
      {/* key={accountNo}: 계좌를 바꾸면 TransactionList 를 새로 만들어 "불러오는 중" 부터 다시 보여 준다 */}
      <TransactionList key={fromAccount.accountNo}
                       accountNo={fromAccount.accountNo}
                       refreshKey={refreshKey}
                       showAmount={showAmount} />
    </Panel>

    <Panel title="AI 카테고리 분류">
      <AiCategory />
    </Panel>

    <Panel title="오늘의 환율"> 
     <ExchangeRate />
    </Panel>

    {/* 화면 오른쪽 아래에 떠 있는 AI 저축 도우미 (로그인한 홈에서만 보인다) */}
    <ChatBot />
    </>
  );
}

// 이 컴포넌트를 외부에서 import해서 쓸 수 있도록 선언
export default BankHome
