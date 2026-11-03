package com.ruvi.saga.infrastructure.messaging;

import com.ruvi.saga.domain.events.SagaCommand;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CommandPublisher {

    private final RabbitTemplate rabbit;
    private final String exchange;

    public CommandPublisher(RabbitTemplate rabbit, @Value("${saga.rabbit.exchange}") String exchange) {
        this.rabbit = rabbit;
        this.exchange = exchange;
    }

    public void send(String routingKey, SagaCommand command) {
        rabbit.convertAndSend(exchange, routingKey, command);
    }
}
