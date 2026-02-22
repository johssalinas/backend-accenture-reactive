package co.com.accenture.model.exception.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessErrorMessage {
    FRANQUICIA_NOT_FOUND("FRA404", "Franquicia no encontrada"),
    INVALID_FRANQUICIA_ID("FRA4001", "El id de la franquicia es inválido"),
    INVALID_FRANQUICIA_NAME("FRA4002", "El nombre de la franquicia es obligatorio"),
    INVALID_FRANQUICIA_REQUEST("FRA4003", "El cuerpo de la solicitud es obligatorio");

    private final String code;
    private final String description;
}