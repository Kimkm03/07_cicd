-- 자바코드가 아닌 것들은 resources 폴더에 넣어야 한다.

-- ============================================================================
--  bankdb 씨드 — 9/28 ERD 정규화 수업이 끝난 시점의 상태
-- ============================================================================
-- 이미 DB에 있습니다
--
--  실행:  mysql -u root -p < data.sql
-- ============================================================================

-- DROP DATABASE IF EXISTS bankdb;
-- CREATE DATABASE bankdb DEFAULT CHARSET = utf8mb4;
--USE bankdb;
--
---- ① 고객 — 계좌 3개가 모두 김연지 한 사람 것입니다 (정규화 결과 1행)
--CREATE TABLE customer (
--    customer_id INT AUTO_INCREMENT PRIMARY KEY,
--    name VARCHAR(50) NOT NULL,
--    grade VARCHAR(10),
--    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
--) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
--
---- ② 고객 연락처 — 연락처가 몇 개든 행만 늘어납니다
--CREATE TABLE customer_phone (
--    customer_id INT NOT NULL,
--    phone VARCHAR(20) NOT NULL,
--    PRIMARY KEY (customer_id, phone),
--    CONSTRAINT fk_phone_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id)
--) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
--
---- ③ 계좌
--CREATE TABLE account (
--    account_id INT AUTO_INCREMENT PRIMARY KEY,
--    customer_id INT NOT NULL,
--    account_no VARCHAR(20) NOT NULL UNIQUE,
--    account_type VARCHAR(10) NOT NULL,
--    balance BIGINT NOT NULL DEFAULT 0,
--    status VARCHAR(10) NOT NULL DEFAULT '정상',
--    opened_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    CONSTRAINT fk_account_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id)
--) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
--
---- ④ 직원
--CREATE TABLE employee (
--    employee_id INT AUTO_INCREMENT PRIMARY KEY,
--    name VARCHAR(50) NOT NULL,
--    duty VARCHAR(10) NOT NULL
--) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
--
---- ⑤ 계좌담당 — 계좌와 직원을 잇는 연결 표 (N:M)
--CREATE TABLE account_manager (
--    account_id INT NOT NULL,
--    employee_id INT NOT NULL,
--    PRIMARY KEY (account_id, employee_id),
--    CONSTRAINT fk_mgr_account FOREIGN KEY (account_id) REFERENCES account (account_id),
--    CONSTRAINT fk_mgr_employee FOREIGN KEY (employee_id) REFERENCES employee (employee_id)
--) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
--
---- ⑥ 이체 — 이체 1건을 한 줄로. idempotency_key 는 같은 요청이 두 번 와도 한 번만 처리되게 막는 칸
--CREATE TABLE transfer (
--    transfer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
--    from_account_id INT NOT NULL,
--    to_account_id INT NULL,
--    amount BIGINT NOT NULL,
--    fee BIGINT NOT NULL DEFAULT 0,
--    status VARCHAR(10) NOT NULL,
--    idempotency_key VARCHAR(64) UNIQUE,
--    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--    CONSTRAINT fk_transfer_from FOREIGN KEY (from_account_id) REFERENCES account (account_id),
--    CONSTRAINT fk_transfer_to FOREIGN KEY (to_account_id) REFERENCES account (account_id)
--) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
--
---- ⑦ 거래 — 🚨 customer_id 가 없습니다. 거래의 주인은 account 를 거쳐 JOIN 으로 찾습니다

---- ----------------------------------------------------------------------------
----  데이터
---- ----------------------------------------------------------------------------
--INSERT INTO customer (customer_id, name, grade) VALUES (1, '김연지', 'VIP');
--
--INSERT INTO customer_phone (customer_id, phone) VALUES
--    (1, '010-1111-2222'),
--    (1, '02-555-0000');

-- 🔐 고객(회원) 2명. 비밀번호는 둘 다 1234 의 BCrypt 해시 (평문은 어디에도 저장하지 않는다)
INSERT INTO customer (customer_id, login_id, password_hash, name, grade, joined_at) VALUES
    (1, 'yeonji', '$2a$10$E2t4RLX1Grd7wyWqmjOPYuOpAAU0asscBfLSD9s1kuUuXn7I9rIU6', '김연지', 'VIP',  '2026-08-01 09:00:00'),
    (2, 'woori',  '$2a$10$E2t4RLX1Grd7wyWqmjOPYuOpAAU0asscBfLSD9s1kuUuXn7I9rIU6', '이우리', '일반', '2026-09-01 09:00:00');
-- 회원가입으로 새 고객이 생기면 3번부터 (H2 문법. MySQL 은 AUTO_INCREMENT 가 알아서 이어 간다)
ALTER TABLE customer ALTER COLUMN customer_id RESTART WITH 3;

INSERT INTO account (account_id, customer_id, account_no, account_type, balance, status) VALUES
    (1, 1, '1002-345-678901', '입출금', 1523000, '지급정지'),
    (2, 1, '1002-345-112233', '적금',   1099500, '정상'),
    (3, 1, '1002-345-998877', '적금',    497000, '휴면');
ALTER TABLE account ALTER COLUMN account_id RESTART WITH 4;
-- 🔐 우리(woori) 고객 계좌 (account_id 는 자동으로 4번이 된다)
INSERT INTO account (customer_id, account_no, account_type, balance, status) VALUES
    (2, '1002-555-667788', '입출금', 500000, '정상');
--INSERT INTO employee (employee_id, name, duty) VALUES
--    (1, '박지훈', '수신'),
--    (2, '이서연', '여신');
--
--INSERT INTO account_manager (account_id, employee_id) VALUES (1, 1), (2, 1), (3, 2);
--
---- 9/28 STEP 13 의 이체 실습 1건 (2번 계좌 → 3번 계좌, 100,000원, 수수료 500원)
--INSERT INTO transfer (transfer_id, from_account_id, to_account_id, amount, fee, status, idempotency_key, requested_at)
--VALUES (1, 2, 3, 100000, 500, '완료', 'demo-transfer-0928-01', '2026-09-20 14:00:00');
--
---- 거래 7건. 1번 계좌의 balance_after 는 "화면(React)에 찍혀 있던 값을 그대로 옮긴 것"이라
---- 서로 맞지 않습니다 — 일부러 그렇게 만든 비정규화 사고 현장입니다. 고치지 마세요.
INSERT INTO `transaction`
    (account_id, transfer_id, tx_type, amount, balance_after, category, memo, counterparty, tx_datetime) VALUES
    (1, NULL, '출금',   45000, 3866000, '쇼핑', '운동화',   '무신사',          '2026-08-31 20:14:00'),
    (1, NULL, '입금', 2400000, 3911000, '급여', '9월급여',  '우리회사',        '2026-09-01 09:00:00'),
    (1, NULL, '출금',   12000, 1511000, '식비', '점심',     '김밥천국',        '2026-09-02 12:31:00'),
    (2, NULL, '입금',  300000,  900000, '저축', '적금납입', '본인',            '2026-08-05 10:00:00'),
    (2, NULL, '입금',  300000, 1200000, '저축', '적금납입', '본인',            '2026-09-05 10:00:00'),
    (2,    1, '출금',  100500, 1099500, '이체', '적금 이체', '1002-345-998877', '2026-09-20 14:00:00'),
    (3,    1, '입금',  100000,  497000, '이체', '적금 이체', '1002-345-112233', '2026-09-20 14:00:00');

---- ----------------------------------------------------------------------------
----  확인 ① 행 수 — 고객 1 / 연락처 2 / 계좌 3 / 직원 2 / 계좌담당 3 / 이체 1 / 거래 7
---- ----------------------------------------------------------------------------
--SELECT (SELECT COUNT(*) FROM customer)        AS 고객,
--       (SELECT COUNT(*) FROM customer_phone)  AS 연락처,
--       (SELECT COUNT(*) FROM account)         AS 계좌,
--       (SELECT COUNT(*) FROM employee)        AS 직원,
--       (SELECT COUNT(*) FROM account_manager) AS 계좌담당,
--       (SELECT COUNT(*) FROM transfer)        AS 이체,
--       (SELECT COUNT(*) FROM `transaction`)   AS 거래;
--
---- ----------------------------------------------------------------------------
----  확인 ② 대사 — 저장된 잔액 = 가장 최근 거래의 balance_after 인가?
----  1번만 12,000원 차이가 나야 정상입니다 (9/28 STEP 13-6 과 같은 쿼리)
---- ----------------------------------------------------------------------------
--SELECT a.account_id, a.account_no, a.balance AS 저장된_잔액,
--       (SELECT t.balance_after FROM `transaction` t
--         WHERE t.account_id = a.account_id
--         ORDER BY t.tx_datetime DESC, t.tx_id DESC LIMIT 1) AS 최신거래_잔액,
--       a.balance - (SELECT t.balance_after FROM `transaction` t
--         WHERE t.account_id = a.account_id
--         ORDER BY t.tx_datetime DESC, t.tx_id DESC LIMIT 1) AS 차이
--FROM account a ORDER BY a.account_id;
--
---- ----------------------------------------------------------------------------
----  확인 ③ 카테고리별 출금 합계 — 이체 100,500 / 쇼핑 45,000 / 식비 12,000
---- ----------------------------------------------------------------------------
--SELECT category, SUM(amount) AS 합계
--FROM `transaction` WHERE tx_type = '출금'
--GROUP BY category ORDER BY 합계 DESC;
