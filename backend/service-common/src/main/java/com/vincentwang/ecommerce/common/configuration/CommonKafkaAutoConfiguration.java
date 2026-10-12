package com.vincentwang.ecommerce.common.configuration;

import com.vincentwang.ecommerce.common.messaging.EventJsonMapper;
import com.vincentwang.ecommerce.common.messaging.KafkaDeadLetterReplayController;
import com.vincentwang.ecommerce.common.messaging.KafkaDeadLetterReplayService;
import com.vincentwang.ecommerce.common.messaging.KafkaEventPublisher;
import java.time.Duration;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration(beforeName = "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration")
@ConditionalOnClass(KafkaTemplate.class)
public class CommonKafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    EventJsonMapper eventJsonMapper(ObjectMapper objectMapper) {
        return new EventJsonMapper(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    KafkaEventPublisher kafkaEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            EventJsonMapper eventJsonMapper,
            @Value("${ecommerce.kafka.send-timeout:10s}") Duration sendTimeout) {
        return new KafkaEventPublisher(kafkaTemplate, eventJsonMapper, sendTimeout);
    }

    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    @ConditionalOnProperty(prefix = "ecommerce.kafka.dlt", name = "enabled", havingValue = "true")
    DefaultErrorHandler kafkaDeadLetterErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${ecommerce.kafka.dlt.retry.initial-interval:250ms}") Duration initialInterval,
            @Value("${ecommerce.kafka.dlt.retry.multiplier:2}") double multiplier,
            @Value("${ecommerce.kafka.dlt.retry.max-interval:2s}") Duration maxInterval,
            @Value("${ecommerce.kafka.dlt.retry.max-attempts:4}") long maxAttempts) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
        ExponentialBackOff backOff = new ExponentialBackOff(initialInterval.toMillis(), multiplier);
        backOff.setMaxInterval(maxInterval.toMillis());
        backOff.setMaxAttempts(maxAttempts);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    @ConditionalOnProperty(prefix = "ecommerce.kafka.dlt", name = "enabled", havingValue = "true")
    ContainerCustomizer<Object, Object, ConcurrentMessageListenerContainer<Object, Object>>
            kafkaDeadLetterContainerCustomizer(DefaultErrorHandler errorHandler) {
        return container -> container.setCommonErrorHandler(errorHandler);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "ecommerce.kafka.dlt", name = "replay-enabled", havingValue = "true")
    KafkaDeadLetterReplayService kafkaDeadLetterReplayService(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${ecommerce.kafka.dlt.allowed-topics:}") String allowedTopics,
            @Value("${ecommerce.kafka.dlt.replay-poll-timeout:5s}") Duration pollTimeout,
            @Value("${ecommerce.kafka.send-timeout:10s}") Duration sendTimeout) {
        return new KafkaDeadLetterReplayService(
                consumerFactory, kafkaTemplate, allowedTopics, pollTimeout, sendTimeout);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "ecommerce.kafka.dlt", name = "replay-enabled", havingValue = "true")
    KafkaDeadLetterReplayController kafkaDeadLetterReplayController(KafkaDeadLetterReplayService replayService) {
        return new KafkaDeadLetterReplayController(replayService);
    }
}
