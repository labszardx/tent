package com.zardx.tent.group.model;

import com.zardx.tent.common.model.MemberStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class Group {

    private String id;

    @NotBlank(message = "Group name is required")
    private String name;

    private String description;
    private Instant createdAt;
    private Long version;

    // Roster
    @Builder.Default
    private List<GroupMember> members = new ArrayList<>();

    // Nirvana State (Accounting)
    @Builder.Default
    private Map<String, UserStats> userStats = new HashMap<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupMember {
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
    public static class UserStats {
        @Builder.Default private BigDecimal paid = BigDecimal.ZERO;
        @Builder.Default private BigDecimal consumed = BigDecimal.ZERO;
        @Builder.Default private BigDecimal balance = BigDecimal.ZERO;
    }
}