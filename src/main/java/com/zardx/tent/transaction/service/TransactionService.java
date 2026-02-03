package com.zardx.tent.transaction.service;

import com.zardx.tent.common.exception.NotAuthorizedException;
import com.zardx.tent.common.model.TransactionType;
import com.zardx.tent.group.persistence.mongo.GroupDocument;
import com.zardx.tent.group.persistence.mongo.GroupRepository;
import com.zardx.tent.group.service.GroupService;
import com.zardx.tent.transaction.mapper.TransactionMapper;
import com.zardx.tent.transaction.model.Transaction;
import com.zardx.tent.transaction.persistence.mongo.TransactionDocument;
import com.zardx.tent.transaction.persistence.mongo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final GroupRepository groupRepository;
    private final GroupService groupService;
    private final TransactionMapper mapper;

    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        // 1. Fetch Group & Payer
        GroupDocument group = groupRepository.findById(transaction.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        GroupDocument.GroupMemberEntity payer = group.getMembers().stream()
                .filter(m -> m.getUserId().equals(transaction.getPayerId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payer is not in the group"));

        // 2. Security Check (Admin Only for Expenses)
        if (transaction.getType() == TransactionType.EXPENSE && !payer.isAdmin()) {
            throw new NotAuthorizedException("Only Admins can add Expenses.");
        }

        // 3. Math Check
        BigDecimal sumSplits = transaction.getSplitDetails().stream()
                .map(Transaction.SplitDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumSplits.compareTo(transaction.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Splits (" + sumSplits + ") do not match total (" + transaction.getTotalAmount() + ")");
        }

        // 4. Save & Update
        TransactionDocument savedDoc = transactionRepository.save(mapper.toEntity(transaction));
        Transaction savedModel = mapper.toDomain(savedDoc);

        // 5. Nirvana Trigger
        groupService.updateBalances(savedModel);

        return savedModel;
    }

    public List<Transaction> getTransactionsByGroup(String groupId) {
        return transactionRepository.findByGroupIdOrderByCreatedAtDesc(groupId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}