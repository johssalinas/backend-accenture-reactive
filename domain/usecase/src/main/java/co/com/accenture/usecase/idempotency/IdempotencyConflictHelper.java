package co.com.accenture.usecase.idempotency;

import co.com.accenture.model.exception.message.BusinessErrorMessage;
import co.com.accenture.model.idempotency.IdempotencyRecord;
import co.com.accenture.model.validation.ReactiveValidationUtils;
import reactor.core.publisher.Mono;

public final class IdempotencyConflictHelper {

    private IdempotencyConflictHelper() {
    }

    public static Mono<Void> validateRequestHash(IdempotencyRecord record, String requestHash) {
        return record.getRequestHash().equals(requestHash)
                ? Mono.empty()
                : ReactiveValidationUtils
                        .businessError(BusinessErrorMessage.IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST);
    }
}
