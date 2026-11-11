package com.example.wonit.service;

import com.example.wonit.dto.AccountCreateRequest;
import com.example.wonit.model.Account;
import com.example.wonit.model.Customer;
import com.example.wonit.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 🔐 회원가입: 고객 저장 + 입출금 계좌 1개 개설을 한 묶음(@Transactional)으로
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;

    public record Joined(Customer customer, Account account) {}

    @Transactional
    public Joined signup(String loginId, String password, String name) {
        if (customerRepository.existsByLoginId(loginId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 있는 아이디입니다.");   // 409
        }
        // 🏦 비밀번호는 BCrypt 해시로만 저장한다. 같은 1234 라도 가입할 때마다 해시가 다르다 (솔트)
        Customer customer = customerRepository.save(Customer.join(loginId, passwordEncoder.encode(password), name));
        // 가입하면 0원 입출금 계좌를 하나 만들어 준다 (홈 화면이 비어 있지 않게). 실패하면 가입도 취소된다
        Account account = accountService.createAccount(new AccountCreateRequest(customer.getCustomerId(), "입출금"));
        return new Joined(customer, account);
    }
}
