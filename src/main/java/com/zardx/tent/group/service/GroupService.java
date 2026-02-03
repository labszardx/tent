package com.zardx.tent.group.service;

import com.zardx.tent.common.exception.NotAuthorizedException;
import com.zardx.tent.common.model.MemberStatus;
import com.zardx.tent.group.mapper.GroupMapper;
import com.zardx.tent.group.model.Group;
import com.zardx.tent.group.model.SettlementPlan;
import com.zardx.tent.group.persistence.mongo.GroupDocument;
import com.zardx.tent.group.persistence.mongo.GroupRepository;
import com.zardx.tent.transaction.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMapper mapper;

    public Group createGroup(Group group) {
        if (group.getOwner() == null) throw new IllegalArgumentException("Owner is required");

        Instant now = Instant.now();

        // 1. Enforce Owner is Super Admin
        group.getOwner().setAdmin(true);
        group.getOwner().setJoinedAt(now);
        group.getOwner().setStatus(MemberStatus.ACTIVE);
        group.getOwner().setAddedBy("SYSTEM");

        // 2. Setup Member List
        if (group.getMembers() == null) group.setMembers(new ArrayList<>());

        // Remove duplicate owner if present in members list
        group.getMembers().removeIf(m -> m.getUserId().equals(group.getOwner().getUserId()));

        // Add owner to top of list
        group.getMembers().add(0, group.getOwner());

        // 3. Initialize status for other members
        group.getMembers().forEach(m -> {
            if (m.getStatus() == null) m.setStatus(MemberStatus.INVITED);
            if (m.getJoinedAt() == null) m.setJoinedAt(now);
        });

        GroupDocument saved = groupRepository.save(mapper.toEntity(group));
        return mapper.toDomain(saved);
    }

    public Group addMember(String groupId, String requesterId, Group.GroupMember newMember) {
        GroupDocument group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // 1. Security: Only Admins can add new people
        boolean isAdmin = group.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(requesterId) && m.isAdmin());

        if (!isAdmin) {
            throw new NotAuthorizedException("Only Admins can add new members.");
        }

        // 2. Validation: Prevent Duplicates
        boolean exists = group.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(newMember.getUserId()));

        if (exists) {
            throw new IllegalArgumentException("User is already in the group.");
        }

        // 3. Initialize New Member
        newMember.setJoinedAt(Instant.now());
        newMember.setAddedBy(requesterId);
        newMember.setStatus(MemberStatus.ACTIVE); // or INVITED
        newMember.setAdmin(false); // Default to regular user

        // 4. Add to Entity List
        // We use a helper from the mapper, or manually map since it's a single item
        GroupDocument.GroupMemberEntity memberEntity = GroupDocument.GroupMemberEntity.builder()
                .userId(newMember.getUserId())
                .nickname(newMember.getNickname())
                .isAdmin(false)
                .joinedAt(Instant.now())
                .addedBy(requesterId)
                .status(MemberStatus.ACTIVE)
                .build();

        group.getMembers().add(memberEntity);

        // 5. Initialize Stats (Important for the Map!)
        group.getUserStats().put(newMember.getUserId(), new GroupDocument.UserStatsEntity());

        GroupDocument saved = groupRepository.save(group);
        return mapper.toDomain(saved);
    }

    public Group getGroup(String id) {
        return groupRepository.findById(id)
                .map(mapper::toDomain)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
    }

    // --- NIRVANA LOGIC ---
    @Transactional
    public void updateBalances(Transaction txn) {
        GroupDocument group = groupRepository.findById(txn.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // 1. Credit Payer
        GroupDocument.UserStatsEntity payerStats = group.getUserStats().computeIfAbsent(txn.getPayerId(), k -> new GroupDocument.UserStatsEntity());
        payerStats.setPaid(payerStats.getPaid().add(txn.getTotalAmount()));
        payerStats.setBalance(payerStats.getBalance().add(txn.getTotalAmount()));

        // 2. Debit Consumers
        for (Transaction.SplitDetail split : txn.getSplitDetails()) {
            GroupDocument.UserStatsEntity consumerStats = group.getUserStats().computeIfAbsent(split.getUserId(), k -> new GroupDocument.UserStatsEntity());
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

        if (!makeAdmin && group.getOwner().getUserId().equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot remove Admin rights from Owner.");
        }

        group.getMembers().stream()
                .filter(m -> m.getUserId().equals(targetUserId))
                .findFirst()
                .ifPresentOrElse(m -> m.setAdmin(makeAdmin),
                        () -> { throw new IllegalArgumentException("User not in group"); });

        return mapper.toDomain(groupRepository.save(group));
    }

    // ... imports

    public SettlementPlan getSettlementPlan(String groupId, String userId) {
        GroupDocument group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        GroupDocument.UserStatsEntity myStats = group.getUserStats().get(userId);
        if (myStats == null) {
            // New user with no stats
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
            BigDecimal myDebt = myBalance.abs(); // e.g., -50 -> 50

            // Find people who are OWED money (Positive balance) to pay them
            group.getUserStats().entrySet().stream()
                    .filter(e -> e.getValue().getBalance().compareTo(BigDecimal.ZERO) > 0) // Filter Creditors
                    .sorted((a, b) -> b.getValue().getBalance().compareTo(a.getValue().getBalance())) // Sort Max Creditors first
                    .forEach(creditor -> {
                        // Logic: I pay them whatever is smaller: My Debt vs Their Receivable
                        // Note: This is a simplifed projection. In a real greedy algo for the whole graph, 
                        // this might vary, but for a personalized view, paying the biggest creditor is safe.
                        // To be mathematically perfect, we usually run the whole group graph simplification, 
                        // but this "Direct Greedy" approach works for 99% of travel groups.

                        // Since we can't mutate 'myDebt' inside the stream easily, we usually iterate with a loop
                        // (See implemented loop below in "Common Logic")
                    });

            // Simplified Greedy Loop for Debtors
            for (Map.Entry<String, GroupDocument.UserStatsEntity> creditor : group.getUserStats().entrySet()) {
                if (myDebt.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal creditorBalance = creditor.getValue().getBalance();
                if (creditorBalance.compareTo(BigDecimal.ZERO) <= 0) continue; // Skip other debtors

                BigDecimal payAmount = myDebt.min(creditorBalance);

                String nick = getNickname(group, creditor.getKey());
                payments.add(SettlementPlan.PaymentAction.builder()
                        .otherUserId(creditor.getKey())
                        .otherUserNickname(nick)
                        .amount(payAmount)
                        .build());

                myDebt = myDebt.subtract(payAmount);
            }
        }

        // --- SCENARIO 2: PEOPLE OWE ME (My Balance is Positive) ---
        else if (myBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal myReceivable = myBalance; // e.g., +100

            // Find people who OWE money (Negative balance) to collect from them
            // Sort by "Biggest Debtors First" (Most negative balance)
            List<Map.Entry<String, GroupDocument.UserStatsEntity>> debtors = group.getUserStats().entrySet().stream()
                    .filter(e -> e.getValue().getBalance().compareTo(BigDecimal.ZERO) < 0)
                    .sorted((a, b) -> a.getValue().getBalance().compareTo(b.getValue().getBalance())) // Ascending (-100 before -10)
                    .collect(Collectors.toList());

            for (Map.Entry<String, GroupDocument.UserStatsEntity> debtor : debtors) {
                if (myReceivable.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal debtorOwes = debtor.getValue().getBalance().abs(); // -40 -> 40

                // Collect whichever is smaller: What they owe OR What I am owed
                BigDecimal collectAmount = myReceivable.min(debtorOwes);

                String nick = getNickname(group, debtor.getKey());
                incoming.add(SettlementPlan.PaymentAction.builder()
                        .otherUserId(debtor.getKey())
                        .otherUserNickname(nick)
                        .amount(collectAmount)
                        .build());

                myReceivable = myReceivable.subtract(collectAmount);
            }
        }

        return SettlementPlan.builder()
                .userId(userId)
                .currentBalance(myBalance)
                .suggestedPayments(payments)   // Populated if I owe
                .expectedIncoming(incoming)    // Populated if I am owed
                .build();
    }

    // Helper to fetch nickname
    private String getNickname(GroupDocument group, String userId) {
        return group.getMembers().stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()                                      // Returns Optional<Member>
                .map(GroupDocument.GroupMemberEntity::getNickname) // Returns Optional<String> (Empty if nick is null)
                .filter(StringUtils::isEmpty) // Ensure it's not just whitespace
                .orElse(userId);                                  // Fallback for all failure cases
    }

    // ... inside GroupService ...

    public Group removeMember(String groupId, String requesterId, String targetUserId) {
        GroupDocument group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        // 1. Authorization Check
        boolean isSelfRemoval = requesterId.equals(targetUserId);
        boolean isRequesterAdmin = group.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(requesterId) && m.isAdmin());

        if (!isSelfRemoval && !isRequesterAdmin) {
            throw new NotAuthorizedException("You do not have permission to remove this member.");
        }

        // 2. Owner Safety Check
        // The owner cannot simply "leave". They must delete the group or transfer ownership.
        if (group.getOwner().getUserId().equals(targetUserId)) {
            throw new IllegalArgumentException("The Owner cannot leave the group. Delete the group or transfer ownership first.");
        }

        // 3. CRITICAL: Zero Balance Check
        // If the map has no entry, balance is considered 0.
        // If entry exists, check the BigDecimals.
        GroupDocument.UserStatsEntity stats = group.getUserStats().get(targetUserId);
        if (stats != null && stats.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            if (stats.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException("Cannot leave: " + targetUserId + " is owed " + stats.getBalance() + ". Settle first.");
            } else {
                throw new IllegalArgumentException("Cannot leave: " + targetUserId + " owes " + stats.getBalance().abs() + ". Settle first.");
            }
        }

        // 4. Execute Removal
        boolean removed = group.getMembers().removeIf(m -> m.getUserId().equals(targetUserId));
        if (!removed) {
            throw new IllegalArgumentException("User not found in group.");
        }

        // Clean up stats map to keep document tidy
        group.getUserStats().remove(targetUserId);

        GroupDocument saved = groupRepository.save(group);
        return mapper.toDomain(saved);
    }
}