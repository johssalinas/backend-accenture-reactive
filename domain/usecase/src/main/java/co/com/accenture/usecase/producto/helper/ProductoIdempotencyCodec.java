package co.com.accenture.usecase.producto.helper;

import co.com.accenture.model.producto.Producto;
import co.com.accenture.model.producto.ProductoStock;
import co.com.accenture.usecase.idempotency.DelimitedIdempotencyPayloadCodec;
import co.com.accenture.usecase.idempotency.IdempotencyPayloadCodec;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ProductoIdempotencyCodec implements IdempotencyPayloadCodec<Producto> {

    private static final DelimitedIdempotencyPayloadCodec<Producto> DELEGATE = DelimitedIdempotencyPayloadCodec.of(
            3,
            value -> Arrays.asList(
                    value.getId() == null ? null : value.getId().toString(),
                    value.getName(),
                    serializeSucursales(value.getSucursales())),
            values -> Producto.builder()
                    .id(values.get(0) == null ? null : UUID.fromString(values.get(0)))
                    .name(values.get(1))
                    .sucursales(deserializeSucursales(values.get(2)))
                    .build());

    public static final ProductoIdempotencyCodec INSTANCE = new ProductoIdempotencyCodec();

    private ProductoIdempotencyCodec() {
    }

    @Override
    public String serialize(Producto value) {
        return DELEGATE.serialize(value);
    }

    @Override
    public Mono<Producto> deserialize(String payload) {
        return DELEGATE.deserialize(payload);
    }

    private static String serializeSucursales(List<ProductoStock> sucursales) {
        if (sucursales == null || sucursales.isEmpty()) {
            return null;
        }

        return sucursales.stream()
                .sorted((a, b) -> String.valueOf(a.getSucursalId()).compareTo(String.valueOf(b.getSucursalId())))
                .map(item -> String.valueOf(item.getSucursalId()) + "@" + String.valueOf(item.getStock()))
                .collect(Collectors.joining(";"));
    }

    private static List<ProductoStock> deserializeSucursales(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(rawValue.split(";"))
                .map(entry -> {
                    String[] values = entry.split("@", 2);
                    return ProductoStock.builder()
                            .sucursalId(values[0] == null || values[0].isBlank() ? null : UUID.fromString(values[0]))
                            .stock(values.length < 2 || values[1] == null || values[1].isBlank() ? null
                                    : Integer.parseInt(values[1]))
                            .build();
                })
                .collect(Collectors.toList());
    }
}
