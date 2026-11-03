package com.ruvi.saga.application.steps;

import com.ruvi.saga.domain.events.SagaCommand;
import com.ruvi.saga.domain.events.SagaResult;
import com.ruvi.saga.infrastructure.messaging.ResultPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String COLLECTION = "payment_attempts";

    private final ResultPublisher results;
    private final MongoTemplate mongo;
    private final MeterRegistry metrics;
    private final double failureRate;
    private final Random random = new Random();

    public PaymentService(ResultPublisher results, MongoTemplate mongo, MeterRegistry metrics,
                          @Value("${saga.simulation.payment-failure-rate:0.3}") double failureRate) {
        this.results = results;
        this.mongo = mongo;
        this.metrics = metrics;
        this.failureRate = failureRate;
    }

    @RabbitListener(queues = "${saga.rabbit.payment-queue}")
    public void onCommand(SagaCommand cmd) {
        if (!"ProcessPayment".equals(cmd.type())) {
            log.warn("PaymentService received unknown command: {}", cmd.type());
            return;
        }
        boolean willFail = random.nextDouble() < failureRate;
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Map<String, Object> doc = new HashMap<>();
        doc.put("_id", paymentId);
        doc.put("orderId", cmd.orderId());
        doc.put("amountMinor", cmd.amountMinor());
        doc.put("currency", cmd.currency());
        doc.put("status", willFail ? "DECLINED" : "APPROVED");
        doc.put("attemptedAt", Instant.now());
        mongo.insert(doc, COLLECTION);

        if (willFail) {
            metrics.counter("payment.failed").increment();
            String reason = "Insufficient funds (simulated, failureRate=" + failureRate + ")";
            log.info("Payment {} DECLINED for order {} — {}", paymentId, cmd.orderId(), reason);
            results.send("payment.failed", SagaResult.paymentFailed(cmd.correlationId(), reason));
        } else {
            metrics.counter("payment.processed").increment();
            log.info("Payment {} APPROVED for order {} ({} {})", paymentId, cmd.orderId(), cmd.amountMinor(), cmd.currency());
            results.send("payment.processed", SagaResult.paymentProcessed(cmd.correlationId(), paymentId));
        }
    }
}
