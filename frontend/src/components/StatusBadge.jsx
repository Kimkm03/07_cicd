// 계좌 상태에 따라 배지 색을 바꿀 겁니다.
const colors = {
    "정상" : "#036c03",
    "휴면" : "#858685",
    "지급정지" : "#fc0808",
    "해지" : "#1f1f1f"
}

// inline(코드 사이에 css를 입힐 때): style={{ key: value }} 형식으로 삽입합니다.
// status 는 Context 가 아니라 props 로 받습니다. "계좌 하나의 상태"는 부모(AccountCard) → 자식 한 단계 값이라서
function StatusBadge({ status }){
    return (
        <span className="badge" style={{ backgroundColor : colors[status] }}>
            {status}
        </span>
    )
} 

export default StatusBadge