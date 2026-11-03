package com.ruvi.saga.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(name = "OrderRequest", description = "Start a new order saga")
public record OrderRequest(
        @Schema(example = "ORDER-9001")
        @NotBlank @Size(max = 64) String orderId,

        @Schema(example = "CUST-42")
        @NotBlank @Size(max = 64) String customerId,

        @Schema(example = "SKU-TSHIRT-BLACK-L")
        @NotBlank @Size(max = 64) String sku,

        @Positive int quantity,

        @Schema(example = "25000", description = "Amount in minor units")
        @Positive long amountMinor,

        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency
) { }
