package com.hmdp.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑：主队列 + 死信队列。
 *
 * 为什么不用 Redis Stream（原版做法）：
 *   Stream 只是"能当队列用"，它没有投递确认语义、没有死信、pending 只能靠人肉清理。
 *   一旦消费者持续失败，消息堆在 pending 里既不重投也不丢弃，最后只能手工 XCLAIM。
 *   选 RabbitMQ 的理由不是"更高级"，而是"它替我把失败消息的归宿定好了"。
 *
 * 为什么不用 Kafka：
 *   单机日订单量到不了需要分区并行的量级，Kafka 的运维成本换不来收益。
 *   RabbitMQ 的 publisher confirm / consumer ack 语义对本场景更贴。
 */
@EnableRabbit
@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "seckill.order.exchange";
    public static final String ORDER_QUEUE = "seckill.order.queue";
    public static final String ORDER_ROUTING_KEY = "seckill.order.create";

    public static final String ORDER_DLX = "seckill.order.dlx";
    public static final String ORDER_DLQ = "seckill.order.dlq";
    public static final String ORDER_DLQ_ROUTING_KEY = "seckill.order.dead";

    @Bean
    public MessageConverter jsonMessageConverter() {
        // 用 JSON 而不是 JDK 序列化：管理台能直接看到消息内容，
        // 排查"消息到底发出去没"时不用反序列化
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                // 消费失败且不再重投时，转入死信交换机，而不是直接丢弃
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding orderBinding() {
        return BindingBuilder.bind(orderQueue()).to(orderExchange()).with(ORDER_ROUTING_KEY);
    }

    @Bean
    public DirectExchange orderDlx() {
        return new DirectExchange(ORDER_DLX, true, false);
    }

    @Bean
    public Queue orderDlq() {
        return QueueBuilder.durable(ORDER_DLQ).build();
    }

    @Bean
    public Binding orderDlqBinding() {
        return BindingBuilder.bind(orderDlq()).to(orderDlx()).with(ORDER_DLQ_ROUTING_KEY);
    }
}
