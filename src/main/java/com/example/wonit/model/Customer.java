package com.example.wonit.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// 🔐 고객(회원) = 로그인하는 사람. 계좌의 customer_id 가 이 표의 customer_id 를 가리킨다
//   수업 data.sql 의 MySQL 설계에 있던 customer 표에 login_id · password_hash 를 더한 것
//   (user · member 는 MySQL 에서 예약어와 겹칠 수 있어 쓰지 않는다)
@Entity
@Table(name = "customer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)      // JPA 용 기본 생성자. 밖에서는 join() 으로만 만든다
public class Customer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "login_id", nullable = false, unique = true, length = 30)   // 같은 아이디는 DB 가 막는다 (UNIQUE)
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 100)   // 🚨 비밀번호는 평문이 아니라 BCrypt 해시만 저장
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "grade", length = 10)
    private String grade;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    // 회원가입: 등급은 "일반" 으로 시작한다
    public static Customer join(String loginId, String passwordHash, String name) {
        Customer customer = new Customer();
        customer.loginId = loginId;
        customer.passwordHash = passwordHash;
        customer.name = name;
        customer.grade = "일반";
        customer.joinedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        return customer;
    }
}
