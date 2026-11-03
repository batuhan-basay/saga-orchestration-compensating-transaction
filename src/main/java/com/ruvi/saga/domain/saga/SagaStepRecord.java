package com.ruvi.saga.domain.saga;

import java.time.Instant;

public record SagaStepRecord(
        SagaStep step,
        StepStatus status,
        String message,
        Instant occurredAt
) {
    public static SagaStepRecord of(SagaStep step, StepStatus status, String message) {
        return new SagaStepRecord(step, status, message, Instant.now());
    }
}
