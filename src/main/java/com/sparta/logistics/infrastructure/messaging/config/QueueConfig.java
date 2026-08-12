package com.sparta.logistics.infrastructure.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {
    @Value("${message.exchange}")
    private String exchange;

    @Value("${message.queue.delivery}")
    private String queueDelivery;
    @Value("${message.queue.hub}")
    private String queueHub;
    @Value("${message.queue.notification}")
    private String queueNotification;

    // 큐 이름을 그대로 routing key로 쓰지 않고, 이벤트 타입별로 routing key를 따로 둔다.
    // notification.queue처럼 여러 도메인이 발행하는 이벤트를 한 큐가 받을 수 있어서,
    // 큐 이름 하나로 뭉뚱그려 바인딩하면 다른 이벤트 타입을 못 받는 문제가 있다.
    @Value("${message.binding-key.notification.delivery-status-changed}")
    private String keyNotificationDeliveryStatusChanged;

    @Bean
    public TopicExchange exchange() { return new TopicExchange(exchange); }

    @Bean public Queue queueDelivery() { return new Queue(queueDelivery); }
    @Bean public Queue queueHub() { return new Queue(queueHub); }
    @Bean public Queue queueNotification() { return new Queue(queueNotification); }

    // Delivery -> Notification (배송 상태 변경)
    @Bean
    public Binding bindingNotificationDeliveryStatusChanged() {
        return BindingBuilder.bind(queueNotification())
                .to(exchange())
                .with(keyNotificationDeliveryStatusChanged);
    }

}
