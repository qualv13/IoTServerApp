package org.qualv13.iotbackend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class RabbitConfig {

    @Value("${mqtt.username}")
    private String username;
    @Value("${mqtt.password}")
    private String password;
    @Value("${mqtt.host:rabbitmq-mqtt-kierzno}")
    private String host;
    @Value("${mqtt.port:5672}")
    private String port;
    //@Value("${mqtt.virtualhost}")
    //private String virtualHost;

    @Value("${mqtt.broker-url:tcp://srv38.mikr.us:40131}")
    private String brokerUrl;

    // Nazwa kolejki, w której będą czekać wiadomości
    public static final String QUEUE_NAME = "lamps_metrics_queue";

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

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        //connectionFactory.setVirtualHost(virtualHost);
        connectionFactory.setHost(host);
        connectionFactory.setPort(Integer.parseInt(port));
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);

        return connectionFactory;
    }

    // 3. Wiążemy kolejkę z Exchangem.
    // Routing Key "lamps.#" oznacza: łap wszystko co zaczyna się od "lamps/"
    // (W MQTT # to wildcard wielopoziomowy)
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("lamps.*.status");
    }
}