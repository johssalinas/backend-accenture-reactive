package co.com.accenture.usecase.franquicia.helper;

import co.com.accenture.model.franquicia.Franquicia;
import co.com.accenture.usecase.idempotency.IdempotencyPayloadCodec;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class FranquiciaIdempotencyCodec implements IdempotencyPayloadCodec<Franquicia> {

    private static final String NULL_TOKEN = "~";

    public static final FranquiciaIdempotencyCodec INSTANCE = new FranquiciaIdempotencyCodec();

    private FranquiciaIdempotencyCodec() {
    }

    @Override
    public String serialize(Franquicia value) {
        if (value == null) {
            return NULL_TOKEN + "|" + NULL_TOKEN;
        }

        String idPart = value.getId() == null ? NULL_TOKEN : value.getId().toString();
        String encodedName = value.getName() == null
                ? NULL_TOKEN
                : Base64.getUrlEncoder().encodeToString(value.getName().getBytes(StandardCharsets.UTF_8));

        return idPart + "|" + encodedName;
    }

    @Override
    public Mono<Franquicia> deserialize(String payload) {
        return Mono.fromSupplier(() -> {
            String[] parts = payload.split("\\|", 2);
            UUID id = NULL_TOKEN.equals(parts[0]) ? null : UUID.fromString(parts[0]);
            String name = NULL_TOKEN.equals(parts[1]) ? null : new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return Franquicia.builder().id(id).name(name).build();
        });
    }
}
