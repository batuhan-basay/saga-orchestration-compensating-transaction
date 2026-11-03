package com.ruvi.saga.infrastructure.messaging;

import com.ruvi.saga.domain.events.SagaResult;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ResultPublisher {

    private final RabbitTemplate rabbit;
    private final String exchange;

    public ResultPublisher(RabbitTemplate rabbit, @Value("${saga.rabbit.results-exchange}") String exchange) {
        this.rabbit = rabbit;
        this.exchange = exchange;
    }

    public void send(String routingKey, SagaResult result) {
        rabbit.convertAndSend(exchange, routingKey, result);
    }
}
