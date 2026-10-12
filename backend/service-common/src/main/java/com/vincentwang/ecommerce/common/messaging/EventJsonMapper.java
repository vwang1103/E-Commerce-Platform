package com.vincentwang.ecommerce.common.messaging;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class EventJsonMapper {

    private final ObjectMapper objectMapper;

    public EventJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(Object value, String failureMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException(failureMessage, exception);
        }
    }

    public EventEnvelope deserializeEnvelope(String message, String failureMessage) {
        try {
            return objectMapper.readValue(message, EventEnvelope.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(failureMessage, exception);
        }
    }

    public JsonNode parsePayload(String payload, String failureMessage) {
        try {
            return objectMapper.readTree(payload);
        } catch (JacksonException exception) {
            throw new IllegalStateException(failureMessage, exception);
        }
    }

    public <T> T convertPayload(JsonNode payload, Class<T> type, String failureMessage) {
        try {
            return objectMapper.treeToValue(payload, type);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(failureMessage, exception);
        }
    }
}
