package com.ruvi.saga.domain.events;

public record SagaResult(
        String type,
        String correlationId,
        boolean success,
        String message,
        String stockReservationId,
        String paymentId
) {
    public static SagaResult stockReserved(String corr, String reservationId) {
        return new SagaResult("StockReserved", corr, true, "Stock reserved", reservationId, null);
    }
    public static SagaResult stockReservationFailed(String corr, String reason) {
        return new SagaResult("StockReservationFailed", corr, false, reason, null, null);
    }
    public static SagaResult paymentProcessed(String corr, String paymentId) {
        return new SagaResult("PaymentProcessed", corr, true, "Payment processed", null, paymentId);
    }
    public static SagaResult paymentFailed(String corr, String reason) {
        return new SagaResult("PaymentFailed", corr, false, reason, null, null);
    }
    public static SagaResult stockCompensated(String corr, String reservationId) {
        return new SagaResult("StockCompensated", corr, true, "Stock reservation reverted", reservationId, null);
    }
}
