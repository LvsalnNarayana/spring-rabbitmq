package com.example.order_service.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReduceResponse {

    private String status; // SUCCESS / FAILED
    private List<UUID> processedProductIds;
    private Instant processedAt;
}
