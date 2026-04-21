package com.zardx.tent.transaction.model;

import com.zardx.tent.common.model.Category;
import com.zardx.tent.common.model.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String id;

    @NotBlank(message = "Group ID is required")
    private String groupId;

    @NotNull(message = "Type is required")
    private TransactionType type;

    private Category category;

    @NotBlank(message = "Payer ID is required")
    private String payerId;

    @NotNull @DecimalMin("0.01")
    private BigDecimal totalAmount;

    private String description;

    private Instant createdAt;
    private String createdBy;

    @NotEmpty @Valid
    private List<SplitDetail> splitDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitDetail {
        @NotBlank private String userId;
        @NotNull @DecimalMin("0.01") private BigDecimal amount;
    }
}