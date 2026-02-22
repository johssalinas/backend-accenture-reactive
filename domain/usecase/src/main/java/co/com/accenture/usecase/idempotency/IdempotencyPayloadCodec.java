package co.com.accenture.usecase.idempotency;

import reactor.core.publisher.Mono;

public interface IdempotencyPayloadCodec<T> {
    String serialize(T value);

    Mono<T> deserialize(String payload);
}
