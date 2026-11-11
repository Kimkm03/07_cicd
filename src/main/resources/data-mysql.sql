-- MySQL 용 데모 데이터 (data.sql 은 H2 문법이라 MySQL 에서는 이 파일을 쓴다)
-- INSERT IGNORE + 번호(PK)를 직접 적는다 → 앱을 다시 켜도 이미 있는 행은 건너뛰어 중복되지 않는다
-- 비밀번호는 둘 다 1234 의 BCrypt 해시
INSERT IGNORE INTO customer (customer_id, login_id, password_hash, name, grade, joined_at) VALUES
    (1, 'yeonji', '$2a$10$E2t4RLX1Grd7wyWqmjOPYuOpAAU0asscBfLSD9s1kuUuXn7I9rIU6', '김연지', 'VIP',  '2026-08-01 09:00:00'),
    (2, 'woori',  '$2a$10$E2t4RLX1Grd7wyWqmjOPYuOpAAU0asscBfLSD9s1kuUuXn7I9rIU6', '이우리', '일반', '2026-09-01 09:00:00');

INSERT IGNORE INTO account (account_id, customer_id, account_no, account_type, balance, status) VALUES
    (1, 1, '1002-345-678901', '입출금', 1523000, '지급정지'),
    (2, 1, '1002-345-112233', '적금',   1099500, '정상'),
    (3, 1, '1002-345-998877', '적금',    497000, '휴면'),
    (4, 2, '1002-555-667788', '입출금',  500000, '정상');

INSERT IGNORE INTO `transaction`
    (tx_id, account_id, transfer_id, tx_type, amount, balance_after, category, memo, counterparty, tx_datetime) VALUES
    (1, 1, NULL, '출금',   45000, 3866000, '쇼핑', '운동화',    '무신사',          '2026-08-31 20:14:00'),
    (2, 1, NULL, '입금', 2400000, 3911000, '급여', '9월급여',   '우리회사',        '2026-09-01 09:00:00'),
    (3, 1, NULL, '출금',   12000, 1511000, '식비', '점심',      '김밥천국',        '2026-09-02 12:31:00'),
    (4, 2, NULL, '입금',  300000,  900000, '저축', '적금납입',  '본인',            '2026-08-05 10:00:00'),
    (5, 2, NULL, '입금',  300000, 1200000, '저축', '적금납입',  '본인',            '2026-09-05 10:00:00'),
    (6, 2,    1, '출금',  100500, 1099500, '이체', '적금 이체', '1002-345-998877', '2026-09-20 14:00:00'),
    (7, 3,    1, '입금',  100000,  497000, '이체', '적금 이체', '1002-345-112233', '2026-09-20 14:00:00');