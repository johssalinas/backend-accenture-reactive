package co.com.accenture.api.config;

import co.com.accenture.model.exception.BusinessException;
import co.com.accenture.model.exception.TechnicalException;
import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.exception.message.TechnicalErrorMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public @NonNull Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable exception) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(exception);
        }

        ErrorPayload payload = toPayload(exception, exchange);
        response.setStatusCode(payload.status());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload.body());
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException jsonProcessingException) {
            byte[] fallbackBytes = "{\"code\":\"TEC5003\",\"message\":\"Ocurrió un error técnico procesando la solicitud\"}"
                    .getBytes(StandardCharsets.UTF_8);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallbackBytes)));
        }
    }

    private ErrorPayload toPayload(Throwable exception, ServerWebExchange exchange) {
        if (exception instanceof BusinessException businessException) {
            BusinessErrorMessage businessError = businessException.getBusinessErrorMessage();
            HttpStatus status = switch (businessError) {
                case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
                case INVALID_RESOURCE_ID,
                        INVALID_RESOURCE_NAME,
                        INVALID_REQUEST_BODY,
                        INVALID_PARENT_RESOURCE_ID,
                        INVALID_CLIENT_ID,
                        INVALID_IDEMPOTENCY_KEY ->
                    HttpStatus.BAD_REQUEST;
                case IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST,
                        IDEMPOTENCY_REQUEST_IN_PROGRESS ->
                    HttpStatus.CONFLICT;
            };
            return payload(status, businessError.getCode(), businessError.getDescription(), exchange);
        }

        if (exception instanceof TechnicalException technicalException) {
            TechnicalErrorMessage technicalError = technicalException.getTechnicalErrorMessage();
            HttpStatus status = switch (technicalError) {
                case DATABASE_CONSTRAINT_ERROR -> HttpStatus.CONFLICT;
                case DATABASE_CONNECTION_ERROR -> HttpStatus.SERVICE_UNAVAILABLE;
                case DATABASE_OPERATION_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            };
            return payload(status, technicalError.getCode(), technicalError.getDescription(), exchange);
        }

        return payload(
                HttpStatus.INTERNAL_SERVER_ERROR,
                TechnicalErrorMessage.DATABASE_OPERATION_ERROR.getCode(),
                TechnicalErrorMessage.DATABASE_OPERATION_ERROR.getDescription(),
                exchange);
    }

    private ErrorPayload payload(HttpStatus status, String code, String message, ServerWebExchange exchange) {
        return new ErrorPayload(
                status,
                Map.of(
                        "timestamp", Instant.now().toString(),
                        "path", exchange.getRequest().getPath().value(),
                        "status", status.value(),
                        "code", code,
                        "message", message,
                        "requestId", exchange.getRequest().getId()));
    }

    private record ErrorPayload(HttpStatus status, Map<String, Object> body) {
    }
}