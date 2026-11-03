package com.ruvi.saga.domain.saga;

public enum SagaStatus {
    STARTED,
    EXECUTING,
    COMPENSATING,
    COMPLETED,
    FAILED
}
