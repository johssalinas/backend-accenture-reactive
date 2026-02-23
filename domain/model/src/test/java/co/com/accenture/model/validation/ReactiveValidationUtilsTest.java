package co.com.accenture.model.validation;

import co.com.accenture.model.exception.BusinessException;
import co.com.accenture.model.exception.message.BusinessErrorMessage;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReactiveValidationUtilsTest {

    @Test
    void requireNonNullShouldEmitValueWhenValueExists() {
        StepVerifier.create(ReactiveValidationUtils.requireNonNull("ok", BusinessErrorMessage.INVALID_REQUEST_BODY))
                .expectNext("ok")
                .verifyComplete();
    }

    @Test
    void requireNonNullShouldReturnBusinessErrorWhenValueIsNull() {
        StepVerifier.create(ReactiveValidationUtils.requireNonNull(null, BusinessErrorMessage.INVALID_REQUEST_BODY))
                .expectErrorSatisfies(error -> {
                    BusinessException businessException = (BusinessException) error;
                    assertEquals(BusinessErrorMessage.INVALID_REQUEST_BODY, businessException.getBusinessErrorMessage());
                })
                .verify();
    }

    @Test
    void requireNonBlankShouldTrimAndEmitValue() {
        StepVerifier.create(ReactiveValidationUtils.requireNonBlank("  Franquicia Norte  ", BusinessErrorMessage.INVALID_RESOURCE_NAME))
                .expectNext("Franquicia Norte")
                .verifyComplete();
    }

    @Test
    void requireNonBlankShouldReturnBusinessErrorWhenValueIsBlank() {
        StepVerifier.create(ReactiveValidationUtils.requireNonBlank("   ", BusinessErrorMessage.INVALID_RESOURCE_NAME))
                .expectErrorSatisfies(error -> {
                    BusinessException businessException = (BusinessException) error;
                    assertEquals(BusinessErrorMessage.INVALID_RESOURCE_NAME, businessException.getBusinessErrorMessage());
                })
                .verify();
    }
}
