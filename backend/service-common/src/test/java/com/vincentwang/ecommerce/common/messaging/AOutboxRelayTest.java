package com.vincentwang.ecommerce.common.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class AOutboxRelayTest {

    private final KafkaEventPublisher publisher = mock(KafkaEventPublisher.class);
    private final EventJsonMapper jsonMapper = new EventJsonMapper(new ObjectMapper());

    record Row(
            String eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            String payload,
            LocalDateTime occurredAt)
            implements IOutboxRecord {}

    private static Row row(String id) {
        return new Row(id, "thing.happened", "thing", "42", "{\"n\":1}", LocalDateTime.of(2026, 1, 2, 3, 4, 5));
    }

    private static final class TestRelay extends AOutboxRelay<Row> {
        final List<Row> pending;
        final List<String> marked = new ArrayList<>();

        TestRelay(KafkaEventPublisher publisher, EventJsonMapper mapper, List<Row> pending) {
            super(publisher, mapper, "Test");
            this.pending = pending;
        }

        @Override
        protected List<Row> claimBatch() {
            return pending;
        }

        @Override
        protected String topicFor(Row event) {
            return "topic." + event.aggregateType();
        }

        @Override
        protected String keyFor(Row event) {
            return event.aggregateId();
        }

        @Override
        protected void markPublished(Row event) {
            marked.add(event.eventId());
        }

        int run() throws Exception {
            return relayBatch();
        }
    }

    @Test
    void publishesEachRowThenMarksIt() throws Exception {
        TestRelay relay = new TestRelay(publisher, jsonMapper, List.of(row("a-b"), row("c-d")));

        assertThat(relay.run()).isEqualTo(2);

        ArgumentCaptor<EventEnvelope> envelope = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(publisher, org.mockito.Mockito.times(2)).publish(eq("topic.thing"), eq("42"), envelope.capture());
        assertThat(envelope.getAllValues().get(0).eventId()).isEqualTo("a-b");
        assertThat(envelope.getAllValues().get(0).traceId()).isEqualTo("ab");
        assertThat(envelope.getAllValues().get(0).schemaVersion()).isEqualTo(1);
        assertThat(envelope.getAllValues().get(0).payload().get("n").asInt()).isEqualTo(1);
        assertThat(relay.marked).containsExactly("a-b", "c-d");
    }

    @Test
    void leavesTheRowUnmarkedWhenThePublishFails() throws Exception {
        TestRelay relay = new TestRelay(publisher, jsonMapper, List.of(row("a-b"), row("c-d")));
        doThrow(new IllegalStateException("broker down")).when(publisher).publish(any(), any(), any());

        assertThatThrownBy(relay::run).hasMessage("broker down");

        assertThat(relay.marked).isEmpty();
        verify(publisher).publish(any(), any(), any());
        verifyNoMoreInteractions(publisher);
    }

    @Test
    void rejectsAStoredPayloadThatIsNotJson() {
        Row broken = new Row("a-b", "thing.happened", "thing", "42", "{not json", LocalDateTime.now());
        TestRelay relay = new TestRelay(publisher, jsonMapper, List.of(broken));

        assertThatThrownBy(relay::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Test outbox contains invalid JSON");
    }
}
