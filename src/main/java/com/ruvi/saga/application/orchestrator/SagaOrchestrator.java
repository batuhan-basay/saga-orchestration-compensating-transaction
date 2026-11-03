package com.ruvi.saga.application.orchestrator;

import com.ruvi.saga.domain.events.SagaCommand;
import com.ruvi.saga.domain.events.SagaResult;
import com.ruvi.saga.domain.saga.SagaInstance;
import com.ruvi.saga.domain.saga.SagaStep;
import com.ruvi.saga.infrastructure.messaging.CommandPublisher;
import com.ruvi.saga.infrastructure.persistence.SagaInstanceRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final SagaInstanceRepository repository;
    private final CommandPublisher commands;
    private final MeterRegistry metrics;

    public SagaOrchestrator(SagaInstanceRepository repository,
                            CommandPublisher commands,
                            MeterRegistry metrics) {
        this.repository = repository;
        this.commands = commands;
        this.metrics = metrics;
    }

    public SagaInstance startOrderSaga(String orderId, String customerId, String sku,
                                       int quantity, long amountMinor, String currency) {
        String correlationId = SagaInstance.newCorrelationId();
        SagaInstance saga = SagaInstance.start(correlationId, orderId, customerId, sku, quantity, amountMinor, currency);
        saga.advanceTo(SagaStep.RESERVE_STOCK);
        saga = repository.save(saga);
        metrics.counter("saga.started").increment();
        log.info("Saga {} started for order {}", correlationId, orderId);
        commands.send("stock.reserve", SagaCommand.reserveStock(correlationId, orderId, sku, quantity));
        return saga;
    }

    @RabbitListener(queues = "${saga.rabbit.results-queue}")
    public void onResult(SagaResult result) {
        var sagaOpt = repository.findByCorrelationId(result.correlationId());
        if (sagaOpt.isEmpty()) {
            log.warn("Received result for unknown saga {}", result.correlationId());
            return;
        }
        SagaInstance saga = sagaOpt.get();
        log.info("Saga {} received {} success={}", saga.getCorrelationId(), result.type(), result.success());

        switch (result.type()) {
            case "StockReserved"           -> onStockReserved(saga, result);
            case "StockReservationFailed"  -> onStockReservationFailed(saga, result);
            case "PaymentProcessed"        -> onPaymentProcessed(saga, result);
            case "PaymentFailed"           -> onPaymentFailed(saga, result);
            case "StockCompensated"        -> onStockCompensated(saga, result);
            default -> log.warn("Unknown result type: {}", result.type());
        }
    }

    private void onStockReserved(SagaInstance saga, SagaResult result) {
        saga.setStockReservationId(result.stockReservationId());
        saga.markStepSuccess(SagaStep.RESERVE_STOCK, "Reservation " + result.stockReservationId());
        saga.advanceTo(SagaStep.PROCESS_PAYMENT);
        repository.save(saga);
        commands.send("payment.process",
                SagaCommand.processPayment(saga.getCorrelationId(), saga.getOrderId(),
                        saga.getAmountMinor(), saga.getCurrency()));
    }

    private void onStockReservationFailed(SagaInstance saga, SagaResult result) {
        saga.markStepFailed(SagaStep.RESERVE_STOCK, result.message());
        saga.failTerminally();
        repository.save(saga);
        metrics.counter("saga.failed", "step", "RESERVE_STOCK").increment();
    }

    private void onPaymentProcessed(SagaInstance saga, SagaResult result) {
        saga.setPaymentId(result.paymentId());
        saga.markStepSuccess(SagaStep.PROCESS_PAYMENT, "Payment " + result.paymentId());
        saga.complete();
        repository.save(saga);
        metrics.counter("saga.completed").increment();
    }

    private void onPaymentFailed(SagaInstance saga, SagaResult result) {
        saga.markStepFailed(SagaStep.PROCESS_PAYMENT, result.message());
        saga.advanceTo(SagaStep.COMPENSATE_STOCK);
        repository.save(saga);
        metrics.counter("saga.compensating", "trigger", "PAYMENT_FAILED").increment();
        commands.send("stock.compensate",
                SagaCommand.compensateStock(saga.getCorrelationId(), saga.getOrderId(),
                        saga.getStockReservationId()));
    }

    private void onStockCompensated(SagaInstance saga, SagaResult result) {
        saga.markStepCompensated(SagaStep.RESERVE_STOCK, "Reversed " + result.stockReservationId());
        saga.failTerminally();
        repository.save(saga);
        metrics.counter("saga.compensated").increment();
    }
}
