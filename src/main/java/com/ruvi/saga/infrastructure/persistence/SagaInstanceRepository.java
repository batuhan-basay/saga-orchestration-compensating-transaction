package com.ruvi.saga.infrastructure.persistence;

import com.ruvi.saga.domain.saga.SagaInstance;
import com.ruvi.saga.domain.saga.SagaStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SagaInstanceRepository extends MongoRepository<SagaInstance, String> {
    Optional<SagaInstance> findByCorrelationId(String correlationId);
    List<SagaInstance> findByStatus(SagaStatus status);
}
