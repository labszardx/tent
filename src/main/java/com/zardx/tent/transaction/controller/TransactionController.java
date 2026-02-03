package com.zardx.tent.transaction.controller;

import com.zardx.tent.transaction.model.Transaction;
import com.zardx.tent.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Transaction> create(@Valid @RequestBody Transaction transaction) {
        return ResponseEntity.ok(transactionService.createTransaction(transaction));
    }

    // ... inside TransactionController ...

    // GET /api/v1/transactions/group/{groupId}
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<Transaction>> getByGroup(@PathVariable String groupId) {
        List<Transaction> transactions = transactionService.getTransactionsByGroup(groupId);
        return ResponseEntity.ok(transactions);
    }
}