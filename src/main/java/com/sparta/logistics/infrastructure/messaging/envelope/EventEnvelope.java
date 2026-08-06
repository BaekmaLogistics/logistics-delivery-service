package com.sparta.logistics.infrastructure.messaging.envelope;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        EventHeader header,
        T payload
) {
    public static <T> EventEnvelope<T> of(String eventType, T payload, UUID actorId) {
        EventHeader header = new EventHeader(
                UUID.randomUUID().toString(),
                actorId,
                eventType,
                Instant.now(),
                "v1"
        );
        return new EventEnvelope<>(header, payload);
    }
}

/*
    {
      "header": {
        "messageId": "msg-12345",
        "eventType": "UserCreated",
        "timestamp": "2026-08-06T10:28:29Z", // Instant
        "version": "v1",
        "actorId": "eac319e5-221e-4064-bcb8-55225ab19c88" // UUID
      },
      "payload": { // ex) UserCreatedPayload
        "userId": "usr-001",
        "email": "user@example.com",
        ...
      }
    }
 */