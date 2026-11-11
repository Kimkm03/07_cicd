// components/TransferForm.jsx
import { useState } from "react"

// 서버가 돌려준 에러 code 로 손님에게 보여 줄 말을 고른다.
// message 를 그대로 보여 줘도 되지만, code 는 프로그램이 읽는 이름이라서 문구를 바꾸거나 다국어로 만들기 쉽다
const MESSAGES = {
  ACCOUNT_FROZEN: "지급정지 계좌라 출금할 수 없습니다.",
  INSUFFICIENT_BALANCE: "잔액이 부족합니다.",
  ACCOUNT_NOT_FOUND: "받는 계좌를 찾을 수 없습니다.",
  NETWORK: "서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.",
}

// 이체 폼 컴포넌트. 하나의 state 객체(form)로 입력값 세 개를
// 한꺼번에 관리하고, handleChange 하나로 세 input 을 모두 처리합니다.
// onTransfer 라는 함수를 부모 컴포넌트에서 props로 전달받습니다. (함수도 코드조각이니까)
function TransferForm({ fromAccount, onTransfer }) {
  const [form, setForm] = useState({ toAccount: "", amount: "", memo: "" })

  // 버튼을 연타해서 이체가 중복으로 나가는 상황을 막기 위해 "전송 중"이라는 상태를 따로 둡니다.
  // 서버와 통신하므로 이 sending 은 응답이 돌아올 때까지 true 로 유지되면서 버튼을 계속 잠궈줍니다.
  const [sending, setSending] = useState(false)

  // 서버가 돌려준 에러 (없으면 null). 문자열이 아니라 에러 객체째로 들고 있어야 code · requestId 를 쓸 수 있다
  const [serverError, setServerError] = useState(null)

  // 멱등성 키(Idempotency-Key): 폼을 열 때 한 번 만들고, 재시도할 때는 같은 값을 쓴다.
  // 서버가 이 키를 기억하면 같은 키로 온 요청을 한 번만 처리할 수 있다 (지금 서버는 읽지 않는다 — Day 1 멱등성 참고). 이체가 성공하면 다음 이체를 위해 새로 만든다
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID())

  function handleChange(e) {
    const { name, value } = e.target

    if (name === "amount") {
      // 숫자만 남기고 나머지 문자는 모두 지웁니다.
      setForm({ ...form, amount: value.replace(/[^0-9]/g, "") })
      return
    }

    setForm({ ...form, [name]: value })
  }

  // 입력값이 하나도 없는 처음 화면에서는 에러 문구를 보여주지 않습니다.
  const touched = form.toAccount !== "" || form.amount !== "" || form.memo !== ""

  let errorMessage = ""
  if (form.toAccount.trim() === "") {
    errorMessage = "받는 계좌를 입력하세요"
  } else if (Number(form.amount) <= 0) {
    errorMessage = "금액을 입력하세요"
  } else if (Number(form.amount) > fromAccount.balance) {
    errorMessage = "잔액이 부족합니다"
  }

  async function handleSubmit(e) {
    e.preventDefault() // 화면의 새로고침을 방지하는 역할
    // HTML의 <form> 태그는 submit 이벤트가 발생할 때 
    // 데이터를 특정 주소로 전송하면서 
    // 브라우저 페이지 전체를 새로고침(Hard Refresh)하는 동작이 기본입니다.
		// React는 화면 전체를 다시 불러오지 않고 필요한 부분만 업데이트하는 SPA(Single Page Application)입니다.
		// 만약 브라우저가 폼 제출 시 페이지를 새로고침해 버리면,
		// React 앱이 처음부터 다시 로드되면서 지금까지 메모리에 저장해 둔 
		// 모든 state(입력 중인 데이터, 로그인 상태 등)가 전부 초기화됩니다.
		// 브라우저의 기본 값인 새로고침 동작을 강제로 막고, 
		// 오직 React의 JavaScript 로직 안에서만 데이터를 안전하게 처리하기 위해 
		// e.preventDefault()를 사용합니다.
    setSending(true)
    setServerError(null)

    try {
      // 서버가 응답할 때까지 기다린다 (await). 그동안 sending 은 true → 버튼이 잠겨 있다
      await onTransfer(
        {
          toAccount: form.toAccount,
          amount: Number(form.amount),
          memo: form.memo,
        },
        idempotencyKey,
      )
      setForm({ toAccount: "", amount: "", memo: "" })
      setIdempotencyKey(crypto.randomUUID())   // 성공했으니 다음 이체는 새 번호표
    } catch (err) {
      setServerError(err)                      // 실패하면 같은 키를 유지 → 재시도해도 두 번 처리되지 않는다
    } finally {
      setSending(false)                        // 응답이 올 때까지 true 로 유지했다가 마지막에 푼다
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <div className="field">
        <label>받는 계좌</label>
        <input
          type="text"
          name="toAccount"
          value={form.toAccount}
          onChange={handleChange}
          placeholder="계좌번호"
        />
      </div>

      <div className="field">
        <label>보낼 금액</label>
        <input
          type="text"
          name="amount"
          value={form.amount === "" ? "" : Number(form.amount).toLocaleString("ko-KR")}
          onChange={handleChange}
          placeholder="0"
          inputMode="numeric"
        />
      </div>

      <div className="field">
        <label>메모 (선택)</label>
        <input
          type="text"
          name="memo"
          value={form.memo}
          onChange={handleChange}
          placeholder="예) 용돈"
        />
      </div>

      {touched && errorMessage !== "" && <p className="error">{errorMessage}</p>}

      {/* 서버가 거절한 이유. 문의 번호(requestId)로 서버 로그에서 이 요청만 찾을 수 있다 */}
      {serverError && (
        <div className="error">
          <p>{MESSAGES[serverError.code] ?? serverError.message}</p>
          {serverError.requestId && (
            <small className="muted">문의 번호: {serverError.requestId}</small>
          )}
        </div>
      )}

      <button className="btn" type="submit" disabled={sending || errorMessage !== ""}>
        {sending ? "이체 중..." : "이체"}
      </button>
    </form>
  )
}

export default TransferForm
