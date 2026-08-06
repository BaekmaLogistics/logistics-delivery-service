package com.sparta.logistics.infrastructure.messaging.envelope;

import java.time.Instant;
import java.util.UUID;

public record EventHeader(
        String messageId,
        UUID actorId,
        String eventType,
        Instant timestamp,
        String version
) {
}