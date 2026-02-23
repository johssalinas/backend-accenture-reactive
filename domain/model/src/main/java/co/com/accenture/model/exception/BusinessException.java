package co.com.accenture.model.exception;

import co.com.accenture.model.exception.message.BusinessErrorMessage;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final BusinessErrorMessage businessErrorMessage;

    public BusinessException(BusinessErrorMessage businessErrorMessage) {
        super(businessErrorMessage.getDescription());
        this.businessErrorMessage = businessErrorMessage;
    }
}