package com.vincentwang.ecommerce.order.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** What the customer chooses. Name, price, and currency are never accepted here; the catalog supplies them. */
public record CreateOrderRequest(@NotEmpty @Size(max = 20) List<@Valid CreateOrderItemRequest> items) {}
