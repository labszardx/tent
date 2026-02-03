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
                .owner(toMemberEntity(model.getOwner()))
                .members(model.getMembers() == null ? new ArrayList<>() :
                        model.getMembers().stream().map(this::toMemberEntity).collect(Collectors.toList()))
                .userStats(model.getUserStats() == null ? new HashMap<>() :
                        model.getUserStats().entrySet().stream().collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> toStatsEntity(e.getValue())
                        )))
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
                .owner(toMemberDomain(entity.getOwner()))
                .members(entity.getMembers() == null ? new ArrayList<>() :
                        entity.getMembers().stream().map(this::toMemberDomain).collect(Collectors.toList()))
                .userStats(entity.getUserStats() == null ? new HashMap<>() :
                        entity.getUserStats().entrySet().stream().collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> toStatsDomain(e.getValue())
                        )))
                .build();
    }

    // --- Helpers ---
    private GroupDocument.GroupMemberEntity toMemberEntity(Group.GroupMember m) {
        return m == null ? null : GroupDocument.GroupMemberEntity.builder()
                .userId(m.getUserId()).isAdmin(m.isAdmin()).joinedAt(m.getJoinedAt())
                .addedBy(m.getAddedBy()).status(m.getStatus()).nickname(m.getNickname()).build();
    }
    private Group.GroupMember toMemberDomain(GroupDocument.GroupMemberEntity e) {
        return e == null ? null : Group.GroupMember.builder()
                .userId(e.getUserId()).isAdmin(e.isAdmin()).joinedAt(e.getJoinedAt())
                .addedBy(e.getAddedBy()).status(e.getStatus()).nickname(e.getNickname()).build();
    }
    private GroupDocument.UserStatsEntity toStatsEntity(Group.UserStats s) {
        return GroupDocument.UserStatsEntity.builder().paid(s.getPaid()).consumed(s.getConsumed()).balance(s.getBalance()).build();
    }
    private Group.UserStats toStatsDomain(GroupDocument.UserStatsEntity e) {
        return Group.UserStats.builder().paid(e.getPaid()).consumed(e.getConsumed()).balance(e.getBalance()).build();
    }
}