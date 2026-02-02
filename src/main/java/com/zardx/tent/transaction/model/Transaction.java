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

    // Null on Request, Populated on Response
    private String id;

    @NotBlank(message = "Group ID is required")
    private String groupId;

    @NotNull(message = "Transaction Type is required")
    private TransactionType type;

    private Category category;

    @NotBlank(message = "Payer ID is required")
    private String payerId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal totalAmount;

    private String description;

    // Null on Request (set by Server), Populated on Response
    private Instant createdAt;

    @NotEmpty(message = "Split details are required")
    @Valid // Cascades validation to the list items
    private List<SplitDetail> splitDetails;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SplitDetail {
        @NotBlank(message = "User ID in split is required")
        private String userId;

        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal amount;
    }
}