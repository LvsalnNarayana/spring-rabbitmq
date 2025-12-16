package com.example.product_service.models;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkInventoryReductionResponse {

    private String status;                 // SUCCESS / FAILED
    private List<UUID> processedProductIds;
    private Instant processedAt;
}
