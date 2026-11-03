package com.ruvi.saga.application.steps;

import com.ruvi.saga.domain.events.SagaCommand;
import com.ruvi.saga.domain.events.SagaResult;
import com.ruvi.saga.infrastructure.messaging.ResultPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);
    private static final String COLLECTION = "stock_reservations";

    private final ResultPublisher results;
    private final MongoTemplate mongo;
    private final MeterRegistry metrics;

    public StockService(ResultPublisher results, MongoTemplate mongo, MeterRegistry metrics) {
        this.results = results;
        this.mongo = mongo;
        this.metrics = metrics;
    }

    @RabbitListener(queues = "${saga.rabbit.stock-queue}")
    public void onCommand(SagaCommand cmd) {
        switch (cmd.type()) {
            case "ReserveStock"    -> reserve(cmd);
            case "CompensateStock" -> compensate(cmd);
            default -> log.warn("StockService received unknown command: {}", cmd.type());
        }
    }

    private void reserve(SagaCommand cmd) {
        String reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> doc = new HashMap<>();
        doc.put("_id", reservationId);
        doc.put("orderId", cmd.orderId());
        doc.put("sku", cmd.sku());
        doc.put("quantity", cmd.quantity());
        doc.put("status", "RESERVED");
        doc.put("reservedAt", Instant.now());
        mongo.insert(doc, COLLECTION);
        metrics.counter("stock.reserved").increment();
        log.info("Stock reserved {} for order {} ({} x {})", reservationId, cmd.orderId(), cmd.quantity(), cmd.sku());
        results.send("stock.reserved", SagaResult.stockReserved(cmd.correlationId(), reservationId));
    }

    private void compensate(SagaCommand cmd) {
        String reservationId = cmd.stockReservationId();
        var query = org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("_id").is(reservationId));
        var update = new org.springframework.data.mongodb.core.query.Update()
                .set("status", "REVERTED")
                .set("revertedAt", Instant.now());
        mongo.updateFirst(query, update, COLLECTION);
        metrics.counter("stock.compensated").increment();
        log.info("Stock reservation {} reverted (compensating transaction)", reservationId);
        results.send("stock.compensated", SagaResult.stockCompensated(cmd.correlationId(), reservationId));
    }
}
