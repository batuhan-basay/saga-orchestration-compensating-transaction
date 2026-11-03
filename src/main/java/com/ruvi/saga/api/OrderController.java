package com.ruvi.saga.api;

import com.ruvi.saga.application.orchestrator.SagaOrchestrator;
import com.ruvi.saga.domain.saga.SagaInstance;
import com.ruvi.saga.infrastructure.persistence.SagaInstanceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Starts and inspects order sagas")
public class OrderController {

    private final SagaOrchestrator orchestrator;
    private final SagaInstanceRepository repository;

    public OrderController(SagaOrchestrator orchestrator, SagaInstanceRepository repository) {
        this.orchestrator = orchestrator;
        this.repository = repository;
    }

    @PostMapping
    @Operation(summary = "Place an order and kick off a saga (ReserveStock → ProcessPayment)")
    public ResponseEntity<SagaInstance> place(@Valid @RequestBody OrderRequest req) {
        SagaInstance saga = orchestrator.startOrderSaga(
                req.orderId(), req.customerId(), req.sku(),
                req.quantity(), req.amountMinor(), req.currency());
        return ResponseEntity.accepted().body(saga);
    }

    @GetMapping("/sagas/{correlationId}")
    @Operation(summary = "Inspect the current state (and full history) of a saga")
    public ResponseEntity<SagaInstance> get(@PathVariable String correlationId) {
        return repository.findByCorrelationId(correlationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
