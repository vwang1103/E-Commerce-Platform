package com.vincentwang.ecommerce.common.messaging;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;

public final class KafkaDeadLetterReplayService {

    private final ConsumerFactory<String, String> consumerFactory;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Set<String> allowedTopics;
    private final Duration pollTimeout;
    private final Duration sendTimeout;

    public KafkaDeadLetterReplayService(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate,
            String allowedTopics,
            Duration pollTimeout,
            Duration sendTimeout) {
        this.consumerFactory = consumerFactory;
        this.kafkaTemplate = kafkaTemplate;
        this.allowedTopics = Arrays.stream(allowedTopics.split(","))
                .map(topic -> topic.trim())
                .filter(topic -> !topic.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.pollTimeout = pollTimeout;
        this.sendTimeout = sendTimeout;
    }

    public DeadLetterReplayResult replay(String deadLetterTopic, int partition, long offset) throws Exception {
        TopicPartition topicPartition = new TopicPartition(deadLetterTopic, partition);
        try (Consumer<String, String> consumer =
                consumerFactory.createConsumer("dlt-replay-" + UUID.randomUUID(), "")) {
            consumer.assign(Set.of(topicPartition));
            consumer.seek(topicPartition, offset);
            ConsumerRecords<String, String> records = consumer.poll(pollTimeout);
            ConsumerRecord<String, String> record = records.records(topicPartition).stream()
                    .filter(candidate -> candidate.offset() == offset)
                    .findFirst()
                    .orElseThrow(() -> new DeadLetterRecordNotFoundException(deadLetterTopic, partition, offset));
            return replay(record);
        }
    }

    public DeadLetterReplayResult replay(ConsumerRecord<String, String> record) throws Exception {
        String destinationTopic = originalTopic(record);
        if (!record.topic().equals(destinationTopic + ".DLT")) {
            throw new IllegalArgumentException("Dead-letter topic does not match original topic metadata");
        }
        if (!allowedTopics.contains(destinationTopic)) {
            throw new IllegalArgumentException("Replay is not allowed for topic " + destinationTopic);
        }

        ProducerRecord<String, String> replay = new ProducerRecord<>(
                destinationTopic, null, record.timestamp(), record.key(), record.value(), new RecordHeaders());
        kafkaTemplate.send(replay).get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
        return new DeadLetterReplayResult(
                record.topic(), record.partition(), record.offset(), destinationTopic, record.key());
    }

    private static String originalTopic(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC);
        if (header == null) {
            throw new IllegalArgumentException("Dead-letter record has no original-topic header");
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    public static final class DeadLetterRecordNotFoundException extends RuntimeException {
        public DeadLetterRecordNotFoundException(String topic, int partition, long offset) {
            super("No dead-letter record at " + topic + "-" + partition + " offset " + offset);
        }
    }
}
