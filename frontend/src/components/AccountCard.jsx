// components/AccountCard.jsx
import StatusBadge from "./StatusBadge";
import { maskAccountNo, formatWonMasked } from "../utils/format";

function AccountCard({ accountNo, accountType, balance, status, showFullNo, showAmount, onDeposit }) {
  return (
    <div className="card">
      {/* console.log('❤️', showAmount) */}
      
      <div className="row">
        <span className="muted">{accountType}</span>
        <StatusBadge status={status} />
      </div>
      <p className="muted">{showFullNo ? accountNo : maskAccountNo(accountNo)}</p>
      <strong className="balance">{ formatWonMasked(balance, showAmount) }</strong>

      {/* Account Card 안에 버튼을 누르면 1만원 입금 추가
       (balance) => { balance + 10000; console.log(balance) 라고 부르면 balance라는 공갈문자로
       화면 위의 이벤트를 사용할 뿐입니다. */}
      <button className="btn" onClick={onDeposit}>1만원 입금</button>
    </div>
  );
}

export default AccountCard
