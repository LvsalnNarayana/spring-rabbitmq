package com.example.order_service.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSnapshot {
    @JsonProperty("id")
    private UUID productId;
    private BigDecimal price;
    private String status; // ACTIVE / OUT_OF_STOCK etc
}
