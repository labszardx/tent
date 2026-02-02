package com.zardx.tent.transaction.mapper;

import com.zardx.tent.transaction.model.Transaction;
import com.zardx.tent.transaction.persistence.mongo.TransactionDocument;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class TransactionMapper {

    // Domain (API) -> Entity (DB)
    public TransactionDocument toEntity(Transaction model) {
        return TransactionDocument.builder()
                .id(model.getId())
                .groupId(model.getGroupId())
                .type(model.getType())
                .category(model.getCategory())
                .payerId(model.getPayerId())
                .totalAmount(model.getTotalAmount())
                .description(model.getDescription())
                .createdAt(model.getCreatedAt())
                .splitDetails(model.getSplitDetails() != null ?
                        model.getSplitDetails().stream()
                                .map(s -> new TransactionDocument.SplitDetailDocument(s.getUserId(), s.getAmount()))
                                .collect(Collectors.toList()) : null)
                .build();
    }

    // Entity (DB) -> Domain (API)
    public Transaction toDomain(TransactionDocument entity) {
        return Transaction.builder()
                .id(entity.getId())
                .groupId(entity.getGroupId())
                .type(entity.getType())
                .category(entity.getCategory())
                .payerId(entity.getPayerId())
                .totalAmount(entity.getTotalAmount())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .splitDetails(entity.getSplitDetails().stream()
                        .map(s -> new Transaction.SplitDetail(s.getUserId(), s.getAmount()))
                        .collect(Collectors.toList()))
                .build();
    }
}