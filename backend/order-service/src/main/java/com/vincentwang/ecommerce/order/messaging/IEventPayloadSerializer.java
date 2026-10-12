package com.vincentwang.ecommerce.order.messaging;

public interface IEventPayloadSerializer {

    String serialize(Object payload);
}
