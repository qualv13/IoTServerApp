package org.qualv13.iotbackend.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Nazwa kolejki, w której będą czekać wiadomości
    public static final String QUEUE_NAME = "iot_lamp_data_queue";

    // 1. Tworzymy trwałą kolejkę (durable = true)
    // Dzięki temu, nawet jak zrestartujesz RabbitMQ, kolejka przetrwa.
    @Bean
    public Queue myQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    // 2. Definiujemy Exchange.
    // DLA MQTT W RABBITMQ TO MUSI BYĆ "amq.topic"!
    @Bean
    public TopicExchange mqttExchange() {
        return new TopicExchange("amq.topic");
    }

    // 3. Wiążemy kolejkę z Exchangem.
    // Routing Key "lamps.#" oznacza: łap wszystko co zaczyna się od "lamps/"
    // (W MQTT # to wildcard wielopoziomowy)
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("lamps.#");
    }
}