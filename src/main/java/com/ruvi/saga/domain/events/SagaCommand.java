package com.ruvi.saga.domain.events;

public record SagaCommand(
        String type,
        String correlationId,
        String orderId,
        String sku,
        int quantity,
        long amountMinor,
        String currency,
        String stockReservationId,
        String paymentId
) {
    public static SagaCommand reserveStock(String corr, String orderId, String sku, int qty) {
        return new SagaCommand("ReserveStock", corr, orderId, sku, qty, 0, null, null, null);
    }
    public static SagaCommand processPayment(String corr, String orderId, long amount, String currency) {
        return new SagaCommand("ProcessPayment", corr, orderId, null, 0, amount, currency, null, null);
    }
    public static SagaCommand compensateStock(String corr, String orderId, String reservationId) {
        return new SagaCommand("CompensateStock", corr, orderId, null, 0, 0, null, reservationId, null);
    }
}
