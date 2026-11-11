package com.example.wonit.controller;

import com.example.wonit.dto.TransactionResponse;
import com.example.wonit.model.Transaction;
import com.example.wonit.service.AccountService;
import com.example.wonit.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    // 생성자 Overriding
    public TransactionController(TransactionService transactionService, AccountService accountService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public List<TransactionResponse> findAll() {
        return transactionService.findAll().stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @GetMapping("/select") //
    public List<TransactionResponse> select(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String memo
    ) {
        boolean hasCategory = StringUtils.hasText(category);
        boolean hasMemo = StringUtils.hasText(memo);

        if (!hasCategory && !hasMemo) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "category 또는 memo를 입력해야 합니다."
            );
        }

        List<?> results = hasMemo
                ? transactionService.getTransactionByCategoryOrMemo(
                hasCategory ? category.trim() : null, memo.trim())
                : transactionService.getTransactionByCategory(category.trim());

        return results.stream()
                .map(Transaction.class::cast)
                .map(TransactionResponse::from)
                .toList();
    }

    @GetMapping("/select2")
    public List<TransactionResponse> selectByAmount(
            @RequestParam long minAmount,
            @RequestParam long maxAmount
    ) {
        if (minAmount > maxAmount) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "minAmount는 maxAmount보다 클 수 없습니다."
            );
        }

        return transactionService.getTransactionByAmount(minAmount, maxAmount).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}