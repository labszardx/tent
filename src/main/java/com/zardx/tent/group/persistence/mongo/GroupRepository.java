package com.zardx.tent.group.persistence.mongo;

import com.zardx.tent.group.model.Group;
import com.zardx.tent.transaction.persistence.mongo.TransactionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends MongoRepository<GroupDocument, String> {
    List<GroupDocument> findByMembersUserId(String userId);
}