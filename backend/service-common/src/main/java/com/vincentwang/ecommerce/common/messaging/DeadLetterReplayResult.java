package com.vincentwang.ecommerce.common.messaging;

public record DeadLetterReplayResult(
        String deadLetterTopic, int deadLetterPartition, long deadLetterOffset, String destinationTopic, String key) {}
