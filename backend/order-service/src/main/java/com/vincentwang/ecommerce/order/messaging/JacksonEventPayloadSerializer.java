package com.vincentwang.ecommerce.order.messaging;

import com.vincentwang.ecommerce.common.messaging.EventJsonMapper;
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
