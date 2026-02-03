package com.zardx.tent.group.persistence.mongo;

import com.zardx.tent.common.model.MemberStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "groups")
public class GroupDocument {

    @Id
    private String id;
    private String name;
    private String description;

    @CreatedDate
    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    @Builder.Default
    private List<GroupMemberEntity> members = new ArrayList<>();

    @Builder.Default
    private List<UserStatsEntity> userStats = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupMemberEntity {
        private String userId;
        private boolean isAdmin;
        private boolean isOwner;
        private Instant joinedAt;
        private String addedBy;
        private MemberStatus status;
        private String nickname;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatsEntity {
        private String userId;
        @Builder.Default private BigDecimal paid = BigDecimal.ZERO;
        @Builder.Default private BigDecimal consumed = BigDecimal.ZERO;
        @Builder.Default private BigDecimal balance = BigDecimal.ZERO;
    }
}