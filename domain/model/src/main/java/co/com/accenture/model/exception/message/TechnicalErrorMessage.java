package co.com.accenture.model.exception.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TechnicalErrorMessage {
    DATABASE_CONNECTION_ERROR("TEC5001", "No fue posible conectarse a la base de datos"),
    DATABASE_CONSTRAINT_ERROR("TEC5002", "Los datos enviados no cumplen las reglas de persistencia"),
    DATABASE_OPERATION_ERROR("TEC5003", "Ocurrió un error técnico procesando la solicitud");

    private final String code;
    private final String description;
}