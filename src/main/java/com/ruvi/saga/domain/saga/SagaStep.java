package com.ruvi.saga.domain.saga;

public enum SagaStep {
    ORDER_CREATED,
    RESERVE_STOCK,
    PROCESS_PAYMENT,
    COMPENSATE_STOCK,
    DONE
}
