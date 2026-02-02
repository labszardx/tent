package com.zardx.tent.transaction.service;

import com.zardx.tent.transaction.mapper.TransactionMapper;
import com.zardx.tent.transaction.model.Transaction;
import com.zardx.tent.transaction.persistence.mongo.TransactionDocument;
import com.zardx.tent.transaction.persistence.mongo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper mapper;

    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        // 1. Business Validation
        BigDecimal sumSplits = transaction.getSplitDetails().stream()
                .map(Transaction.SplitDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumSplits.compareTo(transaction.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Sum of splits (" + sumSplits +
                    ") does not match total amount (" + transaction.getTotalAmount() + ")");
        }

        // 2. Map to Entity
        TransactionDocument entity = mapper.toEntity(transaction);

        // 3. Save
        TransactionDocument savedEntity = transactionRepository.save(entity);

        // 4. Return updated Domain Model (contains the new ID and CreatedAt)
        return mapper.toDomain(savedEntity);
    }
}