package co.com.accenture.model.exception.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessErrorMessage {
    RESOURCE_NOT_FOUND("BUS4040", "Recurso no encontrado"),
    INVALID_RESOURCE_ID("BUS4001", "El identificador del recurso es inválido"),
    INVALID_RESOURCE_NAME("BUS4002", "El nombre del recurso es obligatorio"),
    INVALID_REQUEST_BODY("BUS4003", "El cuerpo de la solicitud es obligatorio"),
    INVALID_PARENT_RESOURCE_ID("BUS4004", "El identificador del recurso padre es inválido"),
    INVALID_CLIENT_ID("BUS4005", "El encabezado X-Client-Id es obligatorio cuando se usa Idempotency-Key"),
    INVALID_IDEMPOTENCY_KEY("BUS4006", "El encabezado Idempotency-Key es inválido"),
    INVALID_RESOURCE_STOCK("BUS4007", "El stock del producto debe ser mayor o igual a cero"),
    IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST("BUS4091",
            "La llave de idempotencia ya fue usada con una solicitud diferente"),
    IDEMPOTENCY_REQUEST_IN_PROGRESS("BUS4092", "Ya existe una solicitud en curso con la misma llave de idempotencia");

    private final String code;
    private final String description;
}