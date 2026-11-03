package com.ruvi.saga.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${saga.rabbit.exchange}")        private String commandsExchange;
    @Value("${saga.rabbit.results-exchange}") private String resultsExchange;
    @Value("${saga.rabbit.stock-queue}")     private String stockQueue;
    @Value("${saga.rabbit.payment-queue}")   private String paymentQueue;
    @Value("${saga.rabbit.results-queue}")   private String resultsQueue;

    @Bean TopicExchange commandsExchange()  { return new TopicExchange(commandsExchange,  true, false); }
    @Bean TopicExchange resultsExchange()   { return new TopicExchange(resultsExchange,   true, false); }

    @Bean Queue stockQueue()   { return QueueBuilder.durable(stockQueue).build(); }
    @Bean Queue paymentQueue() { return QueueBuilder.durable(paymentQueue).build(); }
    @Bean Queue resultsQueue() { return QueueBuilder.durable(resultsQueue).build(); }

    @Bean Binding stockBinding(Queue stockQueue, TopicExchange commandsExchange) {
        return BindingBuilder.bind(stockQueue).to(commandsExchange).with("stock.#");
    }
    @Bean Binding paymentBinding(Queue paymentQueue, TopicExchange commandsExchange) {
        return BindingBuilder.bind(paymentQueue).to(commandsExchange).with("payment.#");
    }
    @Bean Binding resultsBinding(Queue resultsQueue, TopicExchange resultsExchange) {
        return BindingBuilder.bind(resultsQueue).to(resultsExchange).with("#");
    }

    @Bean MessageConverter rabbitMessageConverter(ObjectMapper om) { return new Jackson2JsonMessageConverter(om); }

    @Bean RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter mc) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(mc);
        return t;
    }
}
