package com.zardx.tent.notification.service;

import com.zardx.tent.common.model.Category;
import com.zardx.tent.group.model.Group;
import com.zardx.tent.group.service.GroupService;
import com.zardx.tent.notification.persistence.mongo.Activity;
import com.zardx.tent.notification.persistence.mongo.ActivityRepository;
import com.zardx.tent.transaction.persistence.mongo.TransactionDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final GroupService groupService;

    /**
     * 1) getActivitiesByGroupIds
     * Fetches a paginated list of activities for the groups a user belongs to.
     * Used for the global Activity Feed and the Activity Tab.
     */
    public Page<Activity> getActivitiesByGroupIds(List<String> groupIds, int page, int size) {
        return activityRepository.findByGroupIdInOrderByCreatedAtDesc(
                groupIds,
                PageRequest.of(page, size)
        );
    }

    public void sendNotificationForTxnCreated(TransactionDocument savedDoc) {
        // 1. Fetch group and actor details
        Group group = groupService.getGroup(savedDoc.getGroupId());
        Group.GroupMember addedByMember = group.getMembers().stream()
                .filter(v -> v.getUserId().equals(savedDoc.getCreatedBy()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Member not found"));

        String actorName = addedByMember.getNickname();
        String message;
        Activity.ActivityType activityType;

        // 2. Branch logic based on category/type
        if (Category.SETTLEMENT.equals(savedDoc.getCategory())) {
            // Example: "Rahul recorded a payment of ₹500: Paid via Cash"
            message = String.format("%s recorded a payment of ₹%s: %s",
                    actorName,
                    savedDoc.getTotalAmount().stripTrailingZeros().toPlainString(),
                    savedDoc.getDescription());
            activityType = Activity.ActivityType.SETTLEMENT_COMPLETED;
        } else {
            // Standard Expense: "Rahul added an expense of ₹500 for Dinner"
            message = String.format("%s added an expense of ₹%s for %s",
                    actorName,
                    savedDoc.getTotalAmount().stripTrailingZeros().toPlainString(),
                    savedDoc.getDescription());
            activityType = Activity.ActivityType.TRANSACTION_CREATED;
        }

        // 3. Log the activity with correct type and message
        addActivity(
                savedDoc.getGroupId(),
                savedDoc.getId(),
                savedDoc.getCreatedBy(),
                actorName,
                activityType,
                message
        );
    }

    /**
     * 2) addActivity
     * Standardized method to log events.
     * Populates actor info and timestamps for the "Purple Touch" UI.
     */
    private void addActivity(
            String groupId,
            String targetId,
            String actorId,
            String actorName,
            Activity.ActivityType type,
            String message
    ) {
        Activity activity = Activity.builder()
                .groupId(groupId)
                .targetId(targetId) // Links to specific txn for future deep-linking
                .actorId(actorId)   // ID of person who performed action
                .actorName(actorName)
                .activityType(type)
                .message(message)   // Pre-formatted string (e.g., "Added ₹500 for Dinner")
                .createdAt(Instant.now())
                .build();

        activityRepository.save(activity);
    }
}