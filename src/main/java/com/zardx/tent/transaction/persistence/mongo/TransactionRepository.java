package com.zardx.tent.transaction.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TransactionRepository extends MongoRepository<TransactionDocument, String> {
    List<TransactionDocument> findByGroupIdOrderByCreatedAtDesc(String groupId);
}
