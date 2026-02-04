package com.zardx.tent.group.service;

import com.zardx.tent.common.exception.NotAuthorizedException;
import com.zardx.tent.common.model.MemberStatus;
import com.zardx.tent.group.mapper.GroupMapper;
import com.zardx.tent.group.model.Group;
import com.zardx.tent.group.model.SettlementPlan;
import com.zardx.tent.group.persistence.mongo.GroupDocument;
import com.zardx.tent.group.persistence.mongo.GroupRepository;
import com.zardx.tent.transaction.model.Transaction;
import com.zardx.tent.user.model.User;
import com.zardx.tent.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final AuthService authService;
    private final GroupMapper mapper;

    public Group createGroup(Group group) {
        if (group.getMembers().get(0) == null) throw new IllegalArgumentException("Owner is required");

        Group.GroupMember owner = group.getMembers().get(0);
        Instant now = Instant.now();
        User user = authService.getUserById(owner.getUserId());
        group.setCreatedAt(now);
        group.setUpdatedAt(group.getCreatedAt());

        // 1. Enforce Owner is Super Admin
        owner.setAdmin(true);
        owner.setOwner(true);
        owner.setJoinedAt(now);
        owner.setStatus(MemberStatus.ACTIVE);
        owner.setAddedBy("SYSTEM");
        owner.setNickname(user.getName());


        // 3. Initialize status
        group.getMembers().forEach(m -> {
            if (m.getStatus() == null) m.setStatus(MemberStatus.INVITED);
            if (m.getJoinedAt() == null) m.setJoinedAt(now);
        });

        // 4. Initialize UserStats List (Refactored from Map)
        GroupDocument entity = mapper.toEntity(group);
        if (entity.getUserStats() == null) {
            entity.setUserStats(new ArrayList<>());
        }

        // Ensure owner has a stats entry
        initializeStatsForUser(entity, owner.getUserId());

        GroupDocument saved = groupRepository.save(entity);
        return mapper.toDomain(saved);
    }

    public Group updateGroup(Group group) {

        GroupDocument retrievedGroupDoc = groupRepository.findById(group.getId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        retrievedGroupDoc.setName(group.getName());
        retrievedGroupDoc.setDescription(group.getDescription());
        retrievedGroupDoc.setUpdatedAt(Instant.now());
        GroupDocument saved = groupRepository.save(retrievedGroupDoc);
        return mapper.toDomain(saved);
    }


    public Group addMembers(String groupId, String requesterId, List<Group.GroupMember> newMembers) {
        GroupDocument group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // 1. Security
        boolean isAdmin = group.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(requesterId) && m.isAdmin());

        if (!isAdmin) throw new NotAuthorizedException("Only Admins can add new members.");

        for (Group.GroupMember newMember : newMembers) {
            // 2. Validation
            boolean exists = group.getMembers().stream()
                    .anyMatch(m -> m.getUserId().equals(newMember.getUserId()));

            if (exists) throw new IllegalArgumentException("User " + newMember.getUserId() + " is already in the group.");

            // 3. Initialize New Member
            newMember.setJoinedAt(Instant.now());
            newMember.setAddedBy(requesterId);
            newMember.setStatus(MemberStatus.ACTIVE);
            newMember.setAdmin(false);

            // 4. Add to Member List
            GroupDocument.GroupMemberEntity memberEntity = GroupDocument.GroupMemberEntity.builder()
                    .userId(newMember.getUserId())
                    .nickname(newMember.getNickname())
                    .isAdmin(false)
                    .joinedAt(Instant.now())
                    .addedBy(requesterId)
                    .status(MemberStatus.ACTIVE)
                    .build();

            group.getMembers().add(memberEntity);

            // 5. Initialize Stats (List Logic)
            initializeStatsForUser(group, newMember.getUserId());
        }
        GroupDocument saved = groupRepository.save(group);
        return mapper.toDomain(saved);
    }

    public Group getGroup(String id) {
        return groupRepository.findById(id)
                .map(mapper::toDomain)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
    }

    // --- NIRVANA LOGIC (Refactored for List) ---
    @Transactional
    public void updateBalances(Transaction txn) {
        GroupDocument group = groupRepository.findById(txn.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // 1. Credit Payer
        GroupDocument.UserStatsEntity payerStats = findOrCreateStats(group, txn.getPayerId());
        payerStats.setPaid(payerStats.getPaid().add(txn.getTotalAmount()));
        payerStats.setBalance(payerStats.getBalance().add(txn.getTotalAmount()));

        // 2. Debit Consumers
        for (Transaction.SplitDetail split : txn.getSplitDetails()) {
            GroupDocument.UserStatsEntity consumerStats = findOrCreateStats(group, split.getUserId());
            consumerStats.setConsumed(consumerStats.getConsumed().add(split.getAmount()));
            consumerStats.setBalance(consumerStats.getBalance().subtract(split.getAmount()));
        }

        groupRepository.save(group);
    }

    // --- ACL LOGIC ---
    public Group toggleAdminRights(String groupId, String requesterId, String targetUserId, boolean makeAdmin) {
        GroupDocument group = groupRepository.findById(groupId).orElseThrow(() -> new IllegalArgumentException("Group not found"));

        boolean isRequesterAdmin = group.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(requesterId) && m.isAdmin());

        if (!isRequesterAdmin) throw new NotAuthorizedException("Only Admins can change permissions.");


        if (!makeAdmin && getOwner(group).getUserId().equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot remove Admin rights from Owner.");
        }

        group.getMembers().stream()
                .filter(m -> m.getUserId().equals(targetUserId))
                .findFirst()
                .ifPresentOrElse(m -> m.setAdmin(makeAdmin),
                        () -> { throw new IllegalArgumentException("User not in group"); });

        return mapper.toDomain(groupRepository.save(group));
    }

    public SettlementPlan getSettlementPlan(String groupId, String userId) {
        GroupDocument group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Find my stats in the list
        GroupDocument.UserStatsEntity myStats = group.getUserStats().stream()
                .filter(s -> s.getUserId().equals(userId))
                .findFirst()
                .orElse(null);

        if (myStats == null) {
            return SettlementPlan.builder()
                    .userId(userId)
                    .currentBalance(BigDecimal.ZERO)
                    .suggestedPayments(new ArrayList<>())
                    .expectedIncoming(new ArrayList<>())
                    .build();
        }

        BigDecimal myBalance = myStats.getBalance();
        List<SettlementPlan.PaymentAction> payments = new ArrayList<>();
        List<SettlementPlan.PaymentAction> incoming = new ArrayList<>();

        // --- SCENARIO 1: I OWE MONEY (My Balance is Negative) ---
        if (myBalance.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal myDebt = myBalance.abs();

            // Find creditors (Positive Balance) from the List
            List<GroupDocument.UserStatsEntity> creditors = group.getUserStats().stream()
                    .filter(s -> s.getBalance().compareTo(BigDecimal.ZERO) > 0)
                    .sorted(Comparator.comparing(GroupDocument.UserStatsEntity::getBalance).reversed()) // Biggest creditors first
                    .collect(Collectors.toList());

            for (GroupDocument.UserStatsEntity creditor : creditors) {
                if (myDebt.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal payAmount = myDebt.min(creditor.getBalance());
                String nick = getNickname(group, creditor.getUserId());

                payments.add(SettlementPlan.PaymentAction.builder()
                        .otherUserId(creditor.getUserId())
                        .otherUserNickname(nick)
                        .amount(payAmount)
                        .build());

                myDebt = myDebt.subtract(payAmount);
            }
        }

        // --- SCENARIO 2: PEOPLE OWE ME (My Balance is Positive) ---
        else if (myBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal myReceivable = myBalance;

            // Find debtors (Negative Balance) from the List
            List<GroupDocument.UserStatsEntity> debtors = group.getUserStats().stream()
                    .filter(s -> s.getBalance().compareTo(BigDecimal.ZERO) < 0)
                    .sorted(Comparator.comparing(GroupDocument.UserStatsEntity::getBalance)) // Ascending (Most negative first)
                    .collect(Collectors.toList());

            for (GroupDocument.UserStatsEntity debtor : debtors) {
                if (myReceivable.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal debtorOwes = debtor.getBalance().abs();
                BigDecimal collectAmount = myReceivable.min(debtorOwes);
                String nick = getNickname(group, debtor.getUserId());

                incoming.add(SettlementPlan.PaymentAction.builder()
                        .otherUserId(debtor.getUserId())
                        .otherUserNickname(nick)
                        .amount(collectAmount)
                        .build());

                myReceivable = myReceivable.subtract(collectAmount);
            }
        }

        return SettlementPlan.builder()
                .userId(userId)
                .currentBalance(myBalance)
                .suggestedPayments(payments)
                .expectedIncoming(incoming)
                .build();
    }

    public Group removeMember(String groupId, String requesterId, String targetUserId) {
        GroupDocument group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // 1. Auth Checks
        boolean isSelfRemoval = requesterId.equals(targetUserId);
        boolean isRequesterAdmin = group.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(requesterId) && m.isAdmin());

        boolean isTargetUserAdmin = group.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(targetUserId) && m.isAdmin());

        if (!isSelfRemoval && !isRequesterAdmin) {
            throw new NotAuthorizedException("You do not have permission to remove this member.");
        }
        if (getOwner(group).getUserId().equals(targetUserId)) {
            throw new IllegalArgumentException("The Owner cannot leave the group.");
        }

        if (!isSelfRemoval && isTargetUserAdmin) {
            throw new NotAuthorizedException("You do not have permission to remove this member.");
        }

        // 2. Balance Check (List Logic)
        GroupDocument.UserStatsEntity stats = group.getUserStats().stream()
                .filter(s -> s.getUserId().equals(targetUserId))
                .findFirst()
                .orElse(null);

        if (stats != null && stats.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Cannot leave: User has non-zero balance (" + stats.getBalance() + "). Settle first.");
        }

        // 3. Remove from Members
        boolean removed = group.getMembers().removeIf(m -> m.getUserId().equals(targetUserId));
        if (!removed) throw new IllegalArgumentException("User not found in group.");

        // 4. Remove from Stats List
        group.getUserStats().removeIf(s -> s.getUserId().equals(targetUserId));

        GroupDocument saved = groupRepository.save(group);
        return mapper.toDomain(saved);
    }

    public List<Group> getGroupsByUserId(String userId) {
        List<GroupDocument> byMembersUserId = groupRepository.findByMembersUserId(userId);
        if (CollectionUtils.isEmpty(byMembersUserId)) return Collections.emptyList();

        return byMembersUserId.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    // --- HELPER METHODS ---

    private String getNickname(GroupDocument group, String userId) {
        return group.getMembers().stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .map(GroupDocument.GroupMemberEntity::getNickname)
                .filter(nick -> !nick.trim().isEmpty()) // Ensure it's not null or empty
                .orElse(userId); // Fallback to email if no nickname is set
    }

    private GroupDocument.UserStatsEntity findOrCreateStats(GroupDocument group, String userId) {
        // Initialize list if null (safety check)
        if (group.getUserStats() == null) {
            group.setUserStats(new ArrayList<>());
        }

        return group.getUserStats().stream()
                .filter(s -> s.getUserId().equals(userId))
                .findFirst()
                .orElseGet(() -> {
                    GroupDocument.UserStatsEntity newStats = new GroupDocument.UserStatsEntity();
                    newStats.setUserId(userId);
                    newStats.setBalance(BigDecimal.ZERO);
                    newStats.setPaid(BigDecimal.ZERO);
                    newStats.setConsumed(BigDecimal.ZERO);
                    group.getUserStats().add(newStats);
                    return newStats;
                });
    }

    private void initializeStatsForUser(GroupDocument group, String userId) {
        findOrCreateStats(group, userId);
    }

    private GroupDocument.GroupMemberEntity getOwner(GroupDocument group) {
        for (GroupDocument.GroupMemberEntity member : group.getMembers()) {
            if (member.isOwner()) {
                return member;
            }
        }
        return null;
    }

    public Group updateMemberNickname(String groupId, String userId, String newNickname) {
        GroupDocument group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // Find the member in the list and update the nickname
        group.getMembers().stream()
                .filter(m -> m.getUserId().equalsIgnoreCase(userId))
                .findFirst()
                .ifPresentOrElse(
                        member -> member.setNickname(newNickname),
                        () -> { throw new IllegalArgumentException("User not found in this group"); }
                );

        GroupDocument saved = groupRepository.save(group);
        return mapper.toDomain(saved);
    }
}