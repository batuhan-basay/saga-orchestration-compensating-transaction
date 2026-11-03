package com.ruvi.saga.domain.saga;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "saga_instances")
public class SagaInstance {

    @Id
    private String id;

    @Indexed(unique = true)
    private String correlationId;

    private String sagaType;
    private SagaStatus status;
    private SagaStep currentStep;

    private String orderId;
    private String customerId;
    private String sku;
    private int quantity;
    private long amountMinor;
    private String currency;

    private String stockReservationId;
    private String paymentId;
    private String failureReason;

    private List<SagaStepRecord> history = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    public SagaInstance() { }

    public static SagaInstance start(String correlationId, String orderId, String customerId,
                                     String sku, int quantity, long amountMinor, String currency) {
        SagaInstance s = new SagaInstance();
        s.correlationId = correlationId;
        s.sagaType = "OrderPlacement";
        s.status = SagaStatus.STARTED;
        s.currentStep = SagaStep.ORDER_CREATED;
        s.orderId = orderId;
        s.customerId = customerId;
        s.sku = sku;
        s.quantity = quantity;
        s.amountMinor = amountMinor;
        s.currency = currency;
        s.createdAt = Instant.now();
        s.updatedAt = s.createdAt;
        s.history.add(SagaStepRecord.of(SagaStep.ORDER_CREATED, StepStatus.SUCCESS, "Saga started for order " + orderId));
        return s;
    }

    public void advanceTo(SagaStep next) {
        this.currentStep = next;
        this.status = SagaStatus.EXECUTING;
        touch();
    }

    public void markStepSuccess(SagaStep step, String message) {
        history.add(SagaStepRecord.of(step, StepStatus.SUCCESS, message));
        touch();
    }

    public void markStepFailed(SagaStep step, String reason) {
        this.failureReason = reason;
        this.status = SagaStatus.COMPENSATING;
        history.add(SagaStepRecord.of(step, StepStatus.FAILED, reason));
        touch();
    }

    public void markStepCompensated(SagaStep step, String message) {
        history.add(SagaStepRecord.of(step, StepStatus.COMPENSATED, message));
        touch();
    }

    public void complete() {
        this.status = SagaStatus.COMPLETED;
        this.currentStep = SagaStep.DONE;
        history.add(SagaStepRecord.of(SagaStep.DONE, StepStatus.SUCCESS, "Saga completed"));
        touch();
    }

    public void failTerminally() {
        this.status = SagaStatus.FAILED;
        this.currentStep = SagaStep.DONE;
        history.add(SagaStepRecord.of(SagaStep.DONE, StepStatus.FAILED, failureReason != null ? failureReason : "Saga failed"));
        touch();
    }

    private void touch() { this.updatedAt = Instant.now(); }

    public static String newCorrelationId() { return UUID.randomUUID().toString(); }

    public String getId() { return id; }
    public String getCorrelationId() { return correlationId; }
    public String getSagaType() { return sagaType; }
    public SagaStatus getStatus() { return status; }
    public SagaStep getCurrentStep() { return currentStep; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public long getAmountMinor() { return amountMinor; }
    public String getCurrency() { return currency; }
    public String getStockReservationId() { return stockReservationId; }
    public void setStockReservationId(String stockReservationId) { this.stockReservationId = stockReservationId; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getFailureReason() { return failureReason; }
    public List<SagaStepRecord> getHistory() { return history; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
