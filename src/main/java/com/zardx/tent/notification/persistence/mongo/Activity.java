package com.zardx.tent.notification.persistence.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "activities")
public class Activity {
    @Id
    private String id;

    @Indexed
    private String groupId;

    // The ID of the transaction or member record
    private String targetId;

    // The person who initiated the action
    private String actorId;
    private String actorName;

    private ActivityType activityType;

    // Human-readable summary of the event
    private String message;

    @Indexed
    private Instant createdAt;

    public enum ActivityType {
        TRANSACTION_CREATED,
        TRANSACTION_UPDATED,
        SETTLEMENT_COMPLETED,
        GROUP_CREATED,
        GROUP_UPDATED,
        MEMBER_ADDED,
        MEMBER_REMOVED
    }
}