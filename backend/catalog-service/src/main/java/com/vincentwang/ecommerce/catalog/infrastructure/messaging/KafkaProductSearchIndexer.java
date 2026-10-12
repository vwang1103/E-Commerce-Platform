package com.vincentwang.ecommerce.catalog.infrastructure.messaging;

import com.vincentwang.ecommerce.catalog.application.model.ProductSearchDocument;
import com.vincentwang.ecommerce.catalog.application.repository.IProductSearchRepository;
import com.vincentwang.ecommerce.common.messaging.EventEnvelope;
import com.vincentwang.ecommerce.common.messaging.EventJsonMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "catalog.search.elasticsearch", name = "enabled", havingValue = "true")
public class KafkaProductSearchIndexer {

    private final EventJsonMapper eventJsonMapper;
    private final IProductSearchRepository productSearchRepository;

    public KafkaProductSearchIndexer(
            EventJsonMapper eventJsonMapper, IProductSearchRepository productSearchRepository) {
        this.eventJsonMapper = eventJsonMapper;
        this.productSearchRepository = productSearchRepository;
    }

    @KafkaListener(
            topics = "${catalog.events.product-topic}",
            groupId = "${catalog.search.consumer-group}",
            autoStartup = "${catalog.events.enabled:false}")
    public void index(String message) {
        EventEnvelope envelope = eventJsonMapper.deserializeEnvelope(message, "Product event contains invalid JSON");
        ProductSearchDocument document = eventJsonMapper.convertPayload(
                envelope.payload(), ProductSearchDocument.class, "Product event contains invalid payload");
        productSearchRepository.index(document);
    }
}
