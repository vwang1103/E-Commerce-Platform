package com.vincentwang.ecommerce.order.infrastructure.messaging;

import com.vincentwang.ecommerce.common.messaging.EventJsonMapper;
import com.vincentwang.ecommerce.order.application.IEventPayloadSerializer;
import org.springframework.stereotype.Component;

@Component
public class JacksonEventPayloadSerializer implements IEventPayloadSerializer {

    private final EventJsonMapper eventJsonMapper;

    public JacksonEventPayloadSerializer(EventJsonMapper eventJsonMapper) {
        this.eventJsonMapper = eventJsonMapper;
    }

    @Override
    public String serialize(Object payload) {
        return eventJsonMapper.serialize(payload, "Could not serialize order event");
    }
}
