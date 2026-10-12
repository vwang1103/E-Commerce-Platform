package com.vincentwang.ecommerce.catalog.infrastructure.messaging;

import com.vincentwang.ecommerce.catalog.infrastructure.persistence.IProductMapper;
import com.vincentwang.ecommerce.common.messaging.AOutboxRelay;
import com.vincentwang.ecommerce.common.messaging.EventJsonMapper;
import com.vincentwang.ecommerce.common.messaging.KafkaEventPublisher;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Forwards product changes to one topic, keyed by product ID so each product's events stay ordered. */
@Component
@ConditionalOnProperty(prefix = "catalog.events", name = "enabled", havingValue = "true")
public class KafkaCatalogOutboxPublisher extends AOutboxRelay<CatalogOutboxEvent> {

    private final IProductMapper productMapper;
    private final String topic;
    private final int batchSize;

    public KafkaCatalogOutboxPublisher(
            IProductMapper productMapper,
            EventJsonMapper eventJsonMapper,
            KafkaEventPublisher eventPublisher,
            @Value("${catalog.events.product-topic}") String topic,
            @Value("${catalog.events.outbox-batch-size}") int batchSize) {
        super(eventPublisher, eventJsonMapper, "Catalog");
        this.productMapper = productMapper;
        this.topic = topic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${catalog.events.outbox-poll-delay}")
    @Transactional
    public void publishBatch() throws Exception {
        relayBatch();
    }

    @Override
    protected List<CatalogOutboxEvent> claimBatch() {
        return productMapper.findUnpublishedOutbox(batchSize);
    }

    @Override
    protected String topicFor(CatalogOutboxEvent event) {
        return topic;
    }

    @Override
    protected String keyFor(CatalogOutboxEvent event) {
        return event.aggregateId();
    }

    @Override
    protected void markPublished(CatalogOutboxEvent event) {
        productMapper.markOutboxPublished(event.eventId());
    }
}
