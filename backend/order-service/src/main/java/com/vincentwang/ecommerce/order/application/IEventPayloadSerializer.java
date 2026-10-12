package com.vincentwang.ecommerce.order.application;

public interface IEventPayloadSerializer {

    String serialize(Object payload);
}
