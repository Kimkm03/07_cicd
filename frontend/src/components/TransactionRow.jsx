// 1. 필요로 하는 모듈들을 import 
import { formatWonMasked } from "../utils/format.js"

// 거래 내역 한 줄을 보여주는 컴포넌트
function TransactionRow({ txType, amount, category, memo, counterparty, txDatetime, hideAmount }) {
  return (
    <div className="tx-row">
      <div>
        <strong>{counterparty}</strong>
        <br />
        <span className="muted">
          {category} · {memo}
        </span>
      </div>
      <div>
        <strong className={txType === "입금" ? "amount-in" : "amount-out"}>
          {/* 금액을 숨긴 상태에서는 +/- 부호도 함께 가려서 보여줍니다. */}
          {hideAmount ? "" : txType === "입금" ? "+" : "-"}
          {formatWonMasked(amount, hideAmount)}
        </strong>
        <br />
        <span className="muted">{txDatetime.slice(11, 16)}</span>
      </div>
    </div>
  )
}

export default TransactionRow
