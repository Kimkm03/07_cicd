package com.example.wonit.service;

import com.example.wonit.model.Transaction;
import com.example.wonit.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> findAll() {
        return transactionRepository.findAllByOrderByTxDatetimeDescTxIdDesc();
    }

    public List<Transaction> getTransactionByCategory(String category) {
        return transactionRepository.findByCategoryOrderByTxDatetimeDescTxIdDesc(category);
    }

    public List<Transaction> getTransactionByCategoryOrMemo(String category, String memo) {
        return transactionRepository.searchByCategoryOrMemo(category, memo);
    }

    public List<Transaction> getTransactionByAmount(long minAmount, long maxAmount) {
        return transactionRepository.findByAmountBetweenOrderByTxDatetimeDescTxIdDesc(
                minAmount, maxAmount
        );
    }

}