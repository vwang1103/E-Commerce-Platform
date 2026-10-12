package com.vincentwang.ecommerce.common.messaging;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.kafka.core.KafkaTemplate;

public final class KafkaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventJsonMapper eventJsonMapper;
    private final Duration sendTimeout;

    public KafkaEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate, EventJsonMapper eventJsonMapper, Duration sendTimeout) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventJsonMapper = eventJsonMapper;
        this.sendTimeout = sendTimeout;
    }

    public void publish(String topic, String key, EventEnvelope event) throws Exception {
        String message = eventJsonMapper.serialize(event, "Could not serialize event " + event.eventId());
        kafkaTemplate.send(topic, key, message).get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }
}
