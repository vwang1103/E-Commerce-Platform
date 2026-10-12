package com.vincentwang.ecommerce.common.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class EventJsonMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EventJsonMapper mapper = new EventJsonMapper(objectMapper);

    @Test
    void roundTripsTheSharedEnvelope() {
        EventEnvelope envelope = new EventEnvelope(
                "event-1",
                "product.updated",
                Instant.parse("2026-10-10T00:00:00Z"),
                "trace-1",
                "product",
                "42",
                1,
                objectMapper.createObjectNode().put("productId", 42));

        String json = mapper.serialize(envelope, "serialize failed");

        assertThat(mapper.deserializeEnvelope(json, "deserialize failed")).isEqualTo(envelope);
    }

    @Test
    void givesInvalidInboundJsonAStableExceptionType() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> mapper.deserializeEnvelope("not-json", "invalid event"))
                .withMessage("invalid event");
    }
}
