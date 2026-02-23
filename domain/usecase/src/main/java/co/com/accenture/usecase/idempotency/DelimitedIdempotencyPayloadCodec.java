package co.com.accenture.usecase.idempotency;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

public final class DelimitedIdempotencyPayloadCodec<T> implements IdempotencyPayloadCodec<T> {

    private static final String NULL_TOKEN = "~";

    private final int fieldCount;
    private final Function<T, List<String>> serializer;
    private final Function<List<String>, T> deserializer;

    private DelimitedIdempotencyPayloadCodec(int fieldCount,
            Function<T, List<String>> serializer,
            Function<List<String>, T> deserializer) {
        this.fieldCount = fieldCount;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    public static <T> DelimitedIdempotencyPayloadCodec<T> of(int fieldCount,
            Function<T, List<String>> serializer,
            Function<List<String>, T> deserializer) {
        return new DelimitedIdempotencyPayloadCodec<>(fieldCount, serializer, deserializer);
    }

    @Override
    public String serialize(T value) {
        if (value == null) {
            List<String> emptyFields = new ArrayList<>(fieldCount);
            for (int index = 0; index < fieldCount; index++) {
                emptyFields.add(NULL_TOKEN);
            }
            return String.join("|", emptyFields);
        }

        List<String> fields = serializer.apply(value);
        List<String> normalized = new ArrayList<>(fieldCount);
        for (int index = 0; index < fieldCount; index++) {
            String field = index < fields.size() ? fields.get(index) : null;
            normalized.add(encode(field));
        }
        return String.join("|", normalized);
    }

    @Override
    public Mono<T> deserialize(String payload) {
        return Mono.fromSupplier(() -> {
            String[] parts = payload.split("\\|", fieldCount);
            List<String> decoded = new ArrayList<>(fieldCount);
            for (int index = 0; index < fieldCount; index++) {
                String token = index < parts.length ? parts[index] : NULL_TOKEN;
                decoded.add(decode(token));
            }
            return deserializer.apply(decoded);
        });
    }

    private static String encode(String value) {
        if (value == null) {
            return NULL_TOKEN;
        }
        return Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String token) {
        if (NULL_TOKEN.equals(token)) {
            return null;
        }
        return new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
    }
}