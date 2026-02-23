package co.com.accenture.model.exception.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessErrorMessage {
    FRANQUICIA_NOT_FOUND("FRA404", "Franquicia no encontrada"),
    INVALID_FRANQUICIA_ID("FRA4001", "El id de la franquicia es inválido"),
    INVALID_FRANQUICIA_NAME("FRA4002", "El nombre de la franquicia es obligatorio"),
    INVALID_FRANQUICIA_REQUEST("FRA4003", "El cuerpo de la solicitud es obligatorio"),
    INVALID_CLIENT_ID("FRA4004", "El encabezado X-Client-Id es obligatorio cuando se usa Idempotency-Key"),
    INVALID_IDEMPOTENCY_KEY("FRA4005", "El encabezado Idempotency-Key es inválido"),
    IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST("FRA4091",
            "La llave de idempotencia ya fue usada con una solicitud diferente"),
    IDEMPOTENCY_REQUEST_IN_PROGRESS("FRA4092", "Ya existe una solicitud en curso con la misma llave de idempotencia");

    private final String code;
    private final String description;
}