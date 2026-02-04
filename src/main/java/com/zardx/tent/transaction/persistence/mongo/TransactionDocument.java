package com.zardx.tent.transaction.persistence.mongo;

import com.zardx.tent.common.model.Category;
import com.zardx.tent.common.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
@CompoundIndexes({
        @CompoundIndex(name = "grp_time", def = "{'groupId': 1, 'createdAt': -1}")
})
public class TransactionDocument {
    @Id
    private String id;
    private String groupId;
    private TransactionType type;
    private Category category;
    private String payerId;
    private BigDecimal totalAmount;
    private String description;

    @CreatedDate
    private Instant createdAt;
    private String createdBy;

    private List<SplitDetailDocument> splitDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitDetailDocument {
        private String userId;
        private BigDecimal amount;
    }
}