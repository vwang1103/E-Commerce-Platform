package com.vincentwang.ecommerce.order.messaging;

import com.vincentwang.ecommerce.common.messaging.AOutboxRelay;
import com.vincentwang.ecommerce.common.messaging.EventJsonMapper;
import com.vincentwang.ecommerce.common.messaging.KafkaEventPublisher;
import com.vincentwang.ecommerce.order.repository.mybatis.IOrderMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Routes inventory commands and workflow events to their own topics, each with the key its consumer relies on. */
@Component
@ConditionalOnProperty(prefix = "order.events", name = "enabled", havingValue = "true")
public class KafkaOrderOutboxPublisher extends AOutboxRelay<OrderOutboxEvent> {

    private final IOrderMapper orderMapper;
    private final String workflowTopic;
    private final String inventoryTopic;
    private final int batchSize;

    public KafkaOrderOutboxPublisher(
            IOrderMapper orderMapper,
            EventJsonMapper eventJsonMapper,
            KafkaEventPublisher eventPublisher,
            @Value("${order.events.workflow-topic}") String workflowTopic,
            @Value("${order.events.inventory-topic}") String inventoryTopic,
            @Value("${order.events.outbox-batch-size}") int batchSize) {
        super(eventPublisher, eventJsonMapper, "Order");
        this.orderMapper = orderMapper;
        this.workflowTopic = workflowTopic;
        this.inventoryTopic = inventoryTopic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${order.events.outbox-poll-delay}")
    @Transactional
    public void publishBatch() throws Exception {
        relayBatch();
    }

    @Override
    protected List<OrderOutboxEvent> claimBatch() {
        return orderMapper.findUnpublishedOutbox(batchSize);
    }

    @Override
    protected String topicFor(OrderOutboxEvent event) {
        return OrderEvents.isInventoryCommand(event.eventType()) ? inventoryTopic : workflowTopic;
    }

    @Override
    protected String keyFor(OrderOutboxEvent event) {
        return event.partitionKey();
    }

    @Override
    protected void markPublished(OrderOutboxEvent event) {
        orderMapper.markOutboxPublished(event.userId(), event.eventId());
    }
}
