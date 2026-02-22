package co.com.accenture.model.validation;

import co.com.accenture.model.exception.BusinessException;
import co.com.accenture.model.exception.message.BusinessErrorMessage;
import reactor.core.publisher.Mono;

public final class ReactiveValidationUtils {

    private ReactiveValidationUtils() {
    }

    public static <T> Mono<T> requireNonNull(T value, BusinessErrorMessage businessErrorMessage) {
        return Mono.justOrEmpty(value)
                .switchIfEmpty(businessError(businessErrorMessage));
    }

    public static Mono<String> requireNonBlank(String value, BusinessErrorMessage businessErrorMessage) {
        return Mono.justOrEmpty(value)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .switchIfEmpty(businessError(businessErrorMessage));
    }

    public static <T> Mono<T> businessError(BusinessErrorMessage businessErrorMessage) {
        return Mono.error(() -> new BusinessException(businessErrorMessage));
    }
}