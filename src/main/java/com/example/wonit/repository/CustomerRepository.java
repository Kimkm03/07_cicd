package com.example.wonit.repository;

import com.example.wonit.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByLoginId(String loginId);       // 로그인: 아이디로 찾기

    boolean existsByLoginId(String loginId);                // 회원가입: 이미 있는 아이디인지
}
