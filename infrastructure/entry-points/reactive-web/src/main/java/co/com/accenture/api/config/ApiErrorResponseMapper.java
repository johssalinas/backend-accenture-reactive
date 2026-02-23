package co.com.accenture.api.config;

import co.com.accenture.model.exception.BusinessException;
import co.com.accenture.model.exception.TechnicalException;
import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.exception.message.TechnicalErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

public final class ApiErrorResponseMapper {

    private ApiErrorResponseMapper() {
    }

    public static Mono<ServerResponse> mapBusiness(BusinessException exception) {
        BusinessErrorMessage businessErrorMessage = exception.getBusinessErrorMessage();

        HttpStatus status = switch (businessErrorMessage) {
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_RESOURCE_ID,
                    INVALID_RESOURCE_NAME,
                    INVALID_REQUEST_BODY,
                    INVALID_PARENT_RESOURCE_ID,
                    INVALID_CLIENT_ID,
                    INVALID_IDEMPOTENCY_KEY,
                    INVALID_RESOURCE_STOCK ->
                HttpStatus.BAD_REQUEST;
            case IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST,
                    IDEMPOTENCY_REQUEST_IN_PROGRESS ->
                HttpStatus.CONFLICT;
        };

        return ServerResponse.status(status)
                .bodyValue(Map.of(
                        "code", businessErrorMessage.getCode(),
                        "message", businessErrorMessage.getDescription()));
    }

    public static Mono<ServerResponse> mapTechnical(TechnicalException exception) {
        TechnicalErrorMessage technicalErrorMessage = exception.getTechnicalErrorMessage();

        HttpStatus status = switch (technicalErrorMessage) {
            case DATABASE_CONSTRAINT_ERROR -> HttpStatus.CONFLICT;
            case DATABASE_CONNECTION_ERROR -> HttpStatus.SERVICE_UNAVAILABLE;
            case DATABASE_OPERATION_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        return ServerResponse.status(status)
                .bodyValue(Map.of(
                        "code", technicalErrorMessage.getCode(),
                        "message", technicalErrorMessage.getDescription()));
    }

    public static Mono<ServerResponse> mapUnknown() {
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyValue(Map.of(
                        "code", TechnicalErrorMessage.DATABASE_OPERATION_ERROR.getCode(),
                        "message", TechnicalErrorMessage.DATABASE_OPERATION_ERROR.getDescription()));
    }
}