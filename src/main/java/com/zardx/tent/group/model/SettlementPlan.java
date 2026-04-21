package com.zardx.tent.group.model;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SettlementPlan {
    private String userId;

    // Your Net Position (+100 means you are owed, -50 means you owe)
    private BigDecimal currentBalance;

    // CASE 1: You are a Debtor
    private List<PaymentAction> suggestedPayments;

    // CASE 2: You are a Creditor
    private List<PaymentAction> expectedIncoming;

    @Data
    @Builder
    public static class PaymentAction {
        private String otherUserId;     // The person involved
        private String otherUserNickname;
        private BigDecimal amount;
    }
}