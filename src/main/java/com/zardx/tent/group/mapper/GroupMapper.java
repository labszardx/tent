package com.zardx.tent.group.mapper;

import com.zardx.tent.group.model.Group;
import com.zardx.tent.group.persistence.mongo.GroupDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GroupMapper {

    public GroupDocument toEntity(Group model) {
        if (model == null) return null;

        return GroupDocument.builder()
                .id(model.getId())
                .name(model.getName())
                .description(model.getDescription())
                .createdAt(model.getCreatedAt())
                .version(model.getVersion())
                .members(model.getMembers() == null ? new ArrayList<>() :
                        model.getMembers().stream().map(this::toMemberEntity).collect(Collectors.toList()))

                // --- FIX: Map (Domain) -> List (Entity) ---
                .userStats(model.getUserStats() == null ? new ArrayList<>() :
                        model.getUserStats().entrySet().stream()
                                .map(entry -> {
                                    GroupDocument.UserStatsEntity stats = toStatsEntity(entry.getValue());
                                    stats.setUserId(entry.getKey()); // Important: Set the ID from the Map Key
                                    return stats;
                                })
                                .collect(Collectors.toList()))
                .build();
    }

    public Group toDomain(GroupDocument entity) {
        if (entity == null) return null;

        return Group.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .version(entity.getVersion())
                .members(entity.getMembers() == null ? new ArrayList<>() :
                        entity.getMembers().stream().map(this::toMemberDomain).collect(Collectors.toList()))

                // --- FIX: List (Entity) -> Map (Domain) ---
                .userStats(entity.getUserStats() == null ? new HashMap<>() :
                        entity.getUserStats().stream().collect(Collectors.toMap(
                                GroupDocument.UserStatsEntity::getUserId, // Key = userId from the object
                                this::toStatsDomain
                        )))
                .build();
    }

    // --- Helpers ---
    private GroupDocument.GroupMemberEntity toMemberEntity(Group.GroupMember m) {
        return m == null ? null : GroupDocument.GroupMemberEntity.builder()
                .userId(m.getUserId()).isAdmin(m.isAdmin()).isOwner(m.isOwner()).joinedAt(m.getJoinedAt())
                .addedBy(m.getAddedBy()).status(m.getStatus()).nickname(m.getNickname()).build();
    }
    private Group.GroupMember toMemberDomain(GroupDocument.GroupMemberEntity e) {
        return e == null ? null : Group.GroupMember.builder()
                .userId(e.getUserId()).isAdmin(e.isAdmin()).isOwner(e.isOwner()).joinedAt(e.getJoinedAt())
                .addedBy(e.getAddedBy()).status(e.getStatus()).nickname(e.getNickname()).build();
    }

    private GroupDocument.UserStatsEntity toStatsEntity(Group.UserStats s) {
        // userId is set in the stream above
        return GroupDocument.UserStatsEntity.builder()
                .paid(s.getPaid())
                .consumed(s.getConsumed())
                .balance(s.getBalance())
                .build();
    }
    private Group.UserStats toStatsDomain(GroupDocument.UserStatsEntity e) {
        return Group.UserStats.builder()
                .paid(e.getPaid())
                .consumed(e.getConsumed())
                .balance(e.getBalance())
                .build();
    }
}